import { NavLink } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Sparkles, ShoppingBag, Palette, Camera, User as UserIcon, LogOut } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import logo from "@/assets/logo.png";
import { toast } from "sonner";

const Navigation = () => {
  const { user, loginWithGoogle, logout } = useAuth();
  const navItems = [
    { to: "/try-on", label: "AR Try-On", icon: Camera },
    { to: "/closet", label: "My Closet", icon: ShoppingBag },
    { to: "/outfit-match", label: "Outfit Match", icon: Sparkles },
    { to: "/customize", label: "Customize", icon: Palette },
  ];

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-primary/95 backdrop-blur-sm border-b border-border">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <NavLink to="/" className="flex items-center gap-2">
            <img src={logo} alt="SoleMate Logo" className="h-14 w-auto" />
          </NavLink>

          <div className="hidden md:flex items-center gap-6">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center gap-2 text-sm font-medium transition-colors ${
                    isActive
                      ? "text-foreground"
                      : "text-primary-foreground/80 hover:text-primary-foreground"
                  }`
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </div>

          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <div className="hidden sm:flex flex-col items-end">
                  <span className="text-xs font-medium text-primary-foreground">{user.displayName || 'User'}</span>
                  <span className="text-[10px] text-primary-foreground/60">{user.email}</span>
                </div>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  onClick={async () => {
                    try {
                      await logout();
                      toast.success("Signed out successfully");
                    } catch (err: any) {
                      toast.error("Logout failed: " + err.message);
                    }
                  }}
                  className="text-primary-foreground hover:bg-primary-foreground/10"
                >
                  <LogOut className="h-5 w-5" />
                </Button>
              </div>
            ) : (
              <Button 
                variant="default" 
                onClick={async () => {
                  try {
                    await loginWithGoogle();
                    toast.success("Signed in successfully!");
                  } catch (err: any) {
                    console.error("Login detail err:", err);
                    toast.error("Sign in failed: " + (err.message || "Unknown error"));
                  }
                }}
                className="bg-secondary hover:bg-secondary/90 text-secondary-foreground"
              >
                <UserIcon className="h-4 w-4 mr-2" />
                Sign In
              </Button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navigation;
