import { Link } from 'react-router';
import { Button } from '../components/ui/Button';

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <div className="text-8xl font-black text-crimson-500/20 mb-4 select-none">
        404
      </div>
      <h1 className="text-3xl font-bold text-white mb-2">Page Not Found</h1>
      <p className="text-zinc-400 max-w-md mb-8">
        The screen you are looking for doesn't exist or has been moved to another screen hall.
      </p>
      <Link to="/">
        <Button variant="primary" size="lg">
          Back to Showtime
        </Button>
      </Link>
    </div>
  );
}

export default NotFoundPage;
