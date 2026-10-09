import {
  ReactNode,
  FC,
  SelectHTMLAttributes,
  InputHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";
import { Loader2, X, type LucideIcon } from "lucide-react";
import clsx from "clsx";

//  Spinner
export const Spinner: FC<{ className?: string }> = ({ className }) => (
  <Loader2 className={clsx("animate-spin", className)} />
);

//  Badge
export const Badge: FC<{ children: ReactNode; className?: string }> = ({
  children,
  className,
}) => (
  <span
    className={clsx(
      "inline-flex items-center px-2 py-0.5 rounded text-xs font-medium",
      className,
    )}
  >
    {children}
  </span>
);

//  Avatar
export const Avatar: FC<{
  name?: string;
  src?: string;
  size?: "sm" | "md" | "lg";
  colorClass?: string;
}> = ({ name, src, size = "md", colorClass = "bg-indigo-600" }) => {
  const sizeMap = {
    sm: "w-6 h-6 text-xs",
    md: "w-8 h-8 text-sm",
    lg: "w-10 h-10 text-base",
  };
  if (src)
    return (
      <img
        src={src}
        className={clsx("rounded-full object-cover", sizeMap[size])}
        alt={name}
      />
    );
  return (
    <div
      className={clsx(
        "rounded-full flex items-center justify-center font-semibold text-white flex-shrink-0",
        sizeMap[size],
        colorClass,
      )}
    >
      {name?.[0]?.toUpperCase() ?? "?"}
    </div>
  );
};

//  Modal
export const Modal: FC<{
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  size?: "sm" | "md" | "lg";
}> = ({ open, onClose, title, children, size = "md" }) => {
  if (!open) return null;
  const sizeMap = { sm: "max-w-sm", md: "max-w-lg", lg: "max-w-2xl" };
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm"
        onClick={onClose}
      />
      <div
        className={clsx(
          "relative bg-slate-900 border border-slate-800 rounded-xl w-full p-6 shadow-2xl max-h-[90vh] overflow-y-auto",
          sizeMap[size],
        )}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-slate-100">{title}</h2>
          <button onClick={onClose} className="btn-ghost p-1.5 rounded-lg">
            <X size={16} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
};

//  FormField
export const FormField: FC<{
  label?: string;
  error?: string;
  children: ReactNode;
  hint?: string;
}> = ({ label, error, children, hint }) => (
  <div>
    {label && (
      <label className="block text-xs font-medium text-slate-400 mb-1.5 uppercase tracking-wide">
        {label}
      </label>
    )}
    {children}
    {hint && !error && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    {error && <p className="mt-1 text-xs text-red-400">{error}</p>}
  </div>
);

//  Input
export const Input: FC<InputHTMLAttributes<HTMLInputElement>> = ({
  className,
  ...props
}) => (
  <input
    {...props}
    className={clsx(
      "w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors",
      className,
    )}
  />
);

//  Textarea
export const Textarea: FC<TextareaHTMLAttributes<HTMLTextAreaElement>> = ({
  className,
  ...props
}) => (
  <textarea
    {...props}
    className={clsx(
      "w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors resize-none",
      className,
    )}
  />
);

//  Select
export const Select: FC<
  SelectHTMLAttributes<HTMLSelectElement> & { children: ReactNode }
> = ({ className, children, ...props }) => (
  <select
    {...props}
    className={clsx(
      "w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors",
      className,
    )}
  >
    {children}
  </select>
);

//  Button
interface ButtonProps {
  variant?: "primary" | "ghost" | "danger";
  loading?: boolean;
  children: ReactNode;
  className?: string;
  onClick?: () => void;
  type?: "button" | "submit" | "reset";
  disabled?: boolean;
}
export const Button: FC<ButtonProps> = ({
  variant = "primary",
  loading,
  children,
  className,
  ...props
}) => {
  const base =
    "inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed";
  const variants = {
    primary: "bg-indigo-600 hover:bg-indigo-700 text-white",
    ghost:
      "text-slate-400 hover:text-slate-100 hover:bg-slate-800 border border-slate-700",
    danger:
      "bg-red-600/20 hover:bg-red-600/30 text-red-400 border border-red-600/30",
  };
  return (
    <button
      {...props}
      disabled={props.disabled || loading}
      className={clsx(base, variants[variant], className)}
    >
      {loading && <Spinner className="w-3.5 h-3.5" />}
      {children}
    </button>
  );
};

//  EmptyState
export const EmptyState: FC<{
  icon?: LucideIcon;
  title: string;
  description?: string;
  action?: ReactNode;
}> = ({ icon: Icon, title, description, action }) => (
  <div className="flex flex-col items-center justify-center py-16 text-center">
    {Icon && <Icon size={40} className="text-slate-600 mb-4" />}
    <h3 className="text-slate-300 font-medium mb-1">{title}</h3>
    {description && (
      <p className="text-slate-500 text-sm mb-4">{description}</p>
    )}
    {action}
  </div>
);

//  PageHeader
export const PageHeader: FC<{
  title: string;
  subtitle?: ReactNode;
  action?: ReactNode;
}> = ({ title, subtitle, action }) => (
  <div className="flex items-start justify-between mb-6">
    <div>
      <h1 className="text-2xl font-semibold text-slate-100">{title}</h1>
      {subtitle && <p className="text-slate-400 text-sm mt-1">{subtitle}</p>}
    </div>
    {action}
  </div>
);

//  ErrorBanner
export const ErrorBanner: FC<{ message?: string }> = ({ message }) => {
  if (!message) return null;
  return (
    <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg px-4 py-3 mb-4">
      {message}
    </div>
  );
};
