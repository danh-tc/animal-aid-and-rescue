"use client";

import { FileText } from "lucide-react";
import { useEffect, useState } from "react";
import { apiBlob } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { formatDateTime } from "@/lib/format";
import type { AttachmentView } from "@/types/api";
import "./attachment-list.scss";

const IMAGE_EXT = /\.(jpe?g|png|webp)$/i;

export function AttachmentList({ items }: { items: AttachmentView[] }) {
  if (items.length === 0) return <p className="attachment-list__empty">Chưa có tệp đính kèm.</p>;
  return (
    <ul className="attachment-list">
      {items.map((a) => (
        <AttachmentItem key={a.id} item={a} />
      ))}
    </ul>
  );
}

function AttachmentItem({ item }: { item: AttachmentView }) {
  const { token } = useAuth();
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  const isImage = IMAGE_EXT.test(item.originalName);

  useEffect(() => {
    let objectUrl: string | null = null;
    let cancelled = false;
    apiBlob(`/api/staff/attachments/${item.id}`, token)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => !cancelled && setFailed(true));
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [item.id, token]);

  const content = (
    <>
      <span className="attachment-list__thumb">
        {isImage && url ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={url} alt={item.originalName} width={96} height={96} />
        ) : (
          <FileText aria-hidden="true" />
        )}
      </span>
      <span className="attachment-list__name">{item.originalName}</span>
      <span className="attachment-list__meta">{failed ? "Không tải được tệp" : formatDateTime(item.uploadedAt)}</span>
    </>
  );

  return (
    <li className="attachment-list__item">
      {url ? (
        <a className="attachment-list__link" href={url} target="_blank" rel="noreferrer" download={isImage ? undefined : item.originalName}>
          {content}
        </a>
      ) : (
        <div className="attachment-list__link attachment-list__link--pending">{content}</div>
      )}
    </li>
  );
}
