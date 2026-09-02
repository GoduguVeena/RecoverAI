import { useParams } from 'react-router-dom';

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  return (
    <main className="page-placeholder">
      <h2>Case Detail</h2>
      <p>Case ID: <code>{id}</code></p>
      <p>Full detail view — coming in Step 4.</p>
    </main>
  );
}
