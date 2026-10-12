import { apiBlob } from '../api/client';

export const ALLOWED_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png'];
export const MAX_FILE_BYTES = 5 * 1024 * 1024;

/** Quick check before uploading. The backend checks again (including the file content). */
export function checkFile(file) {
  const ext = file.name.split('.').pop().toLowerCase();
  if (!ALLOWED_EXTENSIONS.includes(ext)) return 'Only PDF, JPG and PNG files are allowed.';
  if (file.size > MAX_FILE_BYTES) return 'File is too large (max 5 MB).';
  if (file.size === 0) return 'The file is empty.';
  return null;
}

/**
 * Files are protected, so a plain <a href> can't be used (it would not send the token).
 * We download the file with the token, then hand it to the browser.
 */
export async function viewDocument(documentId) {
  // Open the tab immediately (inside the click) so pop-up blockers allow it.
  const tab = window.open('', '_blank');
  try {
    const blob = await apiBlob(`/documents/${documentId}/file?inline=true`);
    const url = URL.createObjectURL(blob);
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } catch (err) {
    if (tab) tab.close();
    throw err;
  }
}

export async function downloadDocument(documentId, filename) {
  const blob = await apiBlob(`/documents/${documentId}/file`);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || 'document';
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
