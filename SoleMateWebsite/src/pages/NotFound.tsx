import { useLocation, Link } from "react-router-dom";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Home } from "lucide-react";

const NotFound = () => {
  const location = useLocation();

  useEffect(() => {
    console.error("404 Error: User attempted to access non-existent route:", location.pathname);
  }, [location.pathname]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="text-center max-w-md px-4">
        <h1 className="mb-4 text-8xl font-bold text-primary">404</h1>
        <h2 className="mb-4 text-3xl font-semibold text-foreground">Page Not Found</h2>
        <p className="mb-8 text-muted-foreground">
          Oops! Looks like you've wandered off the path. This page doesn't exist.
        </p>
        <Link to="/">
          <Button className="bg-secondary hover:bg-secondary/90 text-secondary-foreground">
            <Home className="mr-2 h-4 w-4" />
            Return Home
          </Button>
        </Link>
      </div>
    </div>
  );
};

export default NotFound;
