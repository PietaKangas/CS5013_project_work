import { Link, useParams } from 'react-router-dom';
import { useApi } from '../../hooks/useApi';
import LeadDocumentsSection from '../../components/LeadDocumentsSection';
import PageHeader from '../../components/PageHeader';

/** Just the documents of one student, with review actions (the full review page also shows them). */
export default function StudentDocumentsView() {
  const { id } = useParams();
  const { data } = useApi(`/students/${id}`);

  return (
    <>
      <Link className="back-link" to={`/lead/students/${id}`}>← Full student record</Link>
      <PageHeader title={`Documents${data ? ` — ${data.fullName}` : ''}`}
        subtitle="Open each file, then verify it or send it back with a comment." />
      <LeadDocumentsSection studentId={id} />
    </>
  );
}
