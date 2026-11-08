#!/usr/bin/env node

//

//   node scripts/seed-photos.mjs --per-pet 3 --api http://localhost:8080 --email manager@aar.local
//

import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 && args[i + 1] && !args[i + 1].startsWith("--") ? args[i + 1] : fallback;
};
const API = opt("api", "http://localhost:8080");
const EMAIL = opt("email", "manager@aar.local");
const PASSWORD = opt("password", "Password@123");
const PER_PET = Math.min(5, Math.max(1, Number(opt("per-pet", "2"))));
const ONLINE = args.includes("--online");
const PHOTO_DIR = join(dirname(fileURLToPath(import.meta.url)), "..", "backend", "src", "main", "resources", "dev-photos");
const MAX_BYTES = 5 * 1024 * 1024;

async function api(path, { token, method = "GET", body } = {}) {
  const res = await fetch(API + path, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body && !(body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
    },
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${method} ${path} => ${res.status} ${text}`);
  return text ? JSON.parse(text) : null;
}

async function onlinePhoto(species) {
  for (let attempt = 0; attempt < 5; attempt++) {
    const url =
      species === "DOG"
        ? (await (await fetch("https://dog.ceo/api/breeds/image/random")).json()).message
        : (await (await fetch("https://api.thecatapi.com/v1/images/search?mime_types=jpg,png")).json())[0].url;
    if (!/\.(jpe?g|png)$/i.test(url)) continue;
    const res = await fetch(url);
    const bytes = new Uint8Array(await res.arrayBuffer());
    if (res.ok && bytes.length > 0 && bytes.length <= MAX_BYTES) {
      return { name: url.split("/").pop(), type: /\.png$/i.test(url) ? "image/png" : "image/jpeg", bytes };
    }
  }
  throw new Error(`Không lấy được ảnh ${species} trên mạng`);
}

async function localPhoto(file) {
  return { name: file, type: "image/jpeg", bytes: await readFile(join(PHOTO_DIR, file)) };
}

async function main() {
  const manifest = JSON.parse(await readFile(join(PHOTO_DIR, "manifest.json"), "utf8"));
  const { accessToken: token } = await api("/api/public/auth/login", {
    method: "POST",
    body: { email: EMAIL, password: PASSWORD },
  });

  const pets = [];
  for (let page = 0; ; page++) {
    const res = await api(`/api/staff/pets?size=100&page=${page}`, { token });
    pets.push(...res.items);
    if (page + 1 >= res.totalPages) break;
  }

  let done = 0;
  let skipped = 0;
  for (const pet of pets) {
    const detail = await api(`/api/staff/pets/${pet.id}`, { token });
    if (detail.photos.length > 0) {
      skipped++;
      continue;
    }
    let photos;
    if (manifest.pets[pet.code]) {
      photos = await Promise.all(manifest.pets[pet.code].map(localPhoto));
    } else if (ONLINE) {
      photos = [];
      for (let i = 0; i < PER_PET; i++) photos.push(await onlinePhoto(pet.species));
    } else {

      const pool = manifest.pool[pet.species];
      photos = await Promise.all(
        Array.from({ length: Math.min(PER_PET, pool.length) }, (_, i) => localPhoto(pool[(pet.id * 3 + i) % pool.length])),
      );
    }
    const form = new FormData();
    for (const p of photos) form.append("files", new Blob([p.bytes], { type: p.type }), p.name);
    await api(`/api/staff/pets/${pet.id}/attachments`, { token, method: "POST", body: form });
    console.log(`+ ${pet.code} ${pet.name ?? ""}: ${photos.length} ảnh`);
    done++;
  }
  console.log(`Xong: gắn ảnh cho ${done} thú, bỏ qua ${skipped} thú đã có ảnh.`);
}

main().catch((e) => {
  console.error("Lỗi:", e.message);
  process.exit(1);
});
