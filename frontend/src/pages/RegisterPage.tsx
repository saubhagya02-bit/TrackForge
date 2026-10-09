import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { useAuthStore } from "@/store/authStore";

const inputStyle = {
  width: "100%",
  background: "#1e293b",
  border: "1px solid #334155",
  borderRadius: "8px",
  padding: "10px 14px",
  fontSize: "14px",
  color: "#f1f5f9",
  outline: "none",
  boxSizing: "border-box" as const,
  transition: "border-color 0.15s",
};
const labelStyle = {
  display: "block",
  fontSize: "11px",
  fontWeight: 500 as const,
  color: "#64748b",
  textTransform: "uppercase" as const,
  letterSpacing: "0.05em",
  marginBottom: "6px",
};

export default function RegisterPage() {
  const { register } = useAuthStore();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const handle = (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const validate = () => {
    if (!form.username || !form.email || !form.password)
      return "Username, email and password are required";
    if (form.username.length < 3)
      return "Username must be at least 3 characters";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      return "Please enter a valid email address";
    if (form.password.length < 6)
      return "Password must be at least 6 characters";
    if (form.password !== form.confirmPassword) return "Passwords do not match";
    return "";
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const err = validate();
    if (err) {
      setError(err);
      return;
    }
    setError("");
    setLoading(true);
    try {
      await register({
        username: form.username,
        email: form.email,
        password: form.password,
        fullName: form.fullName || undefined,
      });
      navigate("/dashboard");
    } catch (err: any) {
      const msg = err.response?.data;
      setError(
        typeof msg === "string" ? msg : msg?.message || "Registration failed",
      );
    } finally {
      setLoading(false);
    }
  };

  const strength = (() => {
    const p = form.password;
    if (!p) return 0;
    let s = 0;
    if (p.length >= 6) s++;
    if (p.length >= 10) s++;
    if (/[A-Z]/.test(p)) s++;
    if (/[0-9]/.test(p)) s++;
    if (/[^A-Za-z0-9]/.test(p)) s++;
    return s;
  })();
  const strengthColors = [
    "",
    "#ef4444",
    "#f97316",
    "#eab308",
    "#22c55e",
    "#10b981",
  ];
  const strengthLabels = ["", "Weak", "Fair", "Good", "Strong", "Very strong"];

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#020617",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "24px 16px",
      }}
    >
      <div style={{ width: "100%", maxWidth: "380px" }}>
        {/* Logo */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "10px",
            justifyContent: "center",
            marginBottom: "32px",
          }}
        >
          <div
            style={{
              width: "38px",
              height: "38px",
              background: "#4f46e5",
              borderRadius: "10px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "18px",
              boxShadow: "0 0 20px rgba(79,70,229,0.4)",
            }}
          >
            🐛
          </div>
          <span
            style={{
              fontSize: "20px",
              fontWeight: 600,
              color: "#f1f5f9",
              letterSpacing: "-0.02em",
            }}
          >
            TrackForge
          </span>
        </div>

        {/* Card */}
        <div
          style={{
            background: "#0f172a",
            border: "1px solid #1e293b",
            borderRadius: "16px",
            padding: "32px",
            boxShadow: "0 25px 50px rgba(0,0,0,0.5)",
          }}
        >
          <h1
            style={{
              fontSize: "22px",
              fontWeight: 600,
              color: "#f1f5f9",
              marginBottom: "6px",
            }}
          >
            Create account
          </h1>
          <p
            style={{ fontSize: "13px", color: "#64748b", marginBottom: "28px" }}
          >
            Start tracking bugs with your team
          </p>

          {error && (
            <div
              style={{
                background: "rgba(239,68,68,0.1)",
                border: "1px solid rgba(239,68,68,0.3)",
                color: "#f87171",
                fontSize: "13px",
                borderRadius: "8px",
                padding: "10px 14px",
                marginBottom: "16px",
              }}
            >
              {error}
            </div>
          )}

          <form onSubmit={submit}>
            <div
              style={{ display: "flex", flexDirection: "column", gap: "14px" }}
            >
              {/* Full name */}
              <div>
                <label style={labelStyle}>Full name</label>
                <input
                  name="fullName"
                  value={form.fullName}
                  onChange={handle}
                  placeholder="Jane Smith"
                  autoComplete="name"
                  autoFocus
                  style={inputStyle}
                  onFocus={(e) => (e.target.style.borderColor = "#6366f1")}
                  onBlur={(e) => (e.target.style.borderColor = "#334155")}
                />
              </div>

              {/* Username */}
              <div>
                <label style={labelStyle}>
                  Username <span style={{ color: "#f87171" }}>*</span>
                </label>
                <input
                  name="username"
                  value={form.username}
                  onChange={handle}
                  placeholder="janesmith"
                  autoComplete="username"
                  minLength={3}
                  maxLength={50}
                  required
                  style={inputStyle}
                  onFocus={(e) => (e.target.style.borderColor = "#6366f1")}
                  onBlur={(e) => (e.target.style.borderColor = "#334155")}
                />
              </div>

              {/* Email */}
              <div>
                <label style={labelStyle}>
                  Email <span style={{ color: "#f87171" }}>*</span>
                </label>
                <input
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={handle}
                  placeholder="jane@example.com"
                  autoComplete="email"
                  required
                  style={inputStyle}
                  onFocus={(e) => (e.target.style.borderColor = "#6366f1")}
                  onBlur={(e) => (e.target.style.borderColor = "#334155")}
                />
              </div>

              {/* Password */}
              <div>
                <label style={labelStyle}>
                  Password <span style={{ color: "#f87171" }}>*</span>
                </label>
                <div style={{ position: "relative" }}>
                  <input
                    name="password"
                    value={form.password}
                    onChange={handle}
                    type={showPass ? "text" : "password"}
                    placeholder="••••••••"
                    autoComplete="new-password"
                    minLength={6}
                    required
                    style={{ ...inputStyle, paddingRight: "42px" }}
                    onFocus={(e) => (e.target.style.borderColor = "#6366f1")}
                    onBlur={(e) => (e.target.style.borderColor = "#334155")}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPass((s) => !s)}
                    tabIndex={-1}
                    style={{
                      position: "absolute",
                      right: "12px",
                      top: "50%",
                      transform: "translateY(-50%)",
                      background: "none",
                      border: "none",
                      cursor: "pointer",
                      color: "#475569",
                      display: "flex",
                    }}
                  >
                    {showPass ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
                {/* Strength bar */}
                {form.password && (
                  <div style={{ marginTop: "8px" }}>
                    <div
                      style={{
                        display: "flex",
                        gap: "4px",
                        marginBottom: "4px",
                      }}
                    >
                      {[1, 2, 3, 4, 5].map((i) => (
                        <div
                          key={i}
                          style={{
                            height: "3px",
                            flex: 1,
                            borderRadius: "2px",
                            background:
                              i <= strength
                                ? strengthColors[strength]
                                : "#1e293b",
                            transition: "background 0.3s",
                          }}
                        />
                      ))}
                    </div>
                    <span
                      style={{
                        fontSize: "11px",
                        color: strengthColors[strength],
                      }}
                    >
                      {strengthLabels[strength]}
                    </span>
                  </div>
                )}
              </div>

              {/* Confirm password */}
              <div>
                <label style={labelStyle}>
                  Confirm password <span style={{ color: "#f87171" }}>*</span>
                </label>
                <div style={{ position: "relative" }}>
                  <input
                    name="confirmPassword"
                    value={form.confirmPassword}
                    onChange={handle}
                    type={showConfirm ? "text" : "password"}
                    placeholder="••••••••"
                    autoComplete="new-password"
                    required
                    style={{
                      ...inputStyle,
                      paddingRight: "42px",
                      borderColor: form.confirmPassword
                        ? form.password === form.confirmPassword
                          ? "#22c55e"
                          : "#ef4444"
                        : "#334155",
                    }}
                    onFocus={(e) => {
                      if (!form.confirmPassword)
                        e.target.style.borderColor = "#6366f1";
                    }}
                    onBlur={(e) => {
                      e.target.style.borderColor = form.confirmPassword
                        ? form.password === form.confirmPassword
                          ? "#22c55e"
                          : "#ef4444"
                        : "#334155";
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirm((s) => !s)}
                    tabIndex={-1}
                    style={{
                      position: "absolute",
                      right: "12px",
                      top: "50%",
                      transform: "translateY(-50%)",
                      background: "none",
                      border: "none",
                      cursor: "pointer",
                      color: "#475569",
                      display: "flex",
                    }}
                  >
                    {showConfirm ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
                {form.confirmPassword &&
                  form.password !== form.confirmPassword && (
                    <p
                      style={{
                        color: "#f87171",
                        fontSize: "11px",
                        marginTop: "4px",
                      }}
                    >
                      Passwords do not match
                    </p>
                  )}
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={
                loading ||
                (!!form.confirmPassword &&
                  form.password !== form.confirmPassword)
              }
              style={{
                width: "100%",
                marginTop: "24px",
                background: loading ? "#4338ca" : "#4f46e5",
                border: "none",
                borderRadius: "8px",
                padding: "11px",
                fontSize: "14px",
                fontWeight: 500,
                color: "#fff",
                cursor: loading ? "not-allowed" : "pointer",
                transition: "background 0.15s",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "8px",
              }}
              onMouseEnter={(e) => {
                if (!loading)
                  (e.target as HTMLElement).style.background = "#4338ca";
              }}
              onMouseLeave={(e) => {
                if (!loading)
                  (e.target as HTMLElement).style.background = "#4f46e5";
              }}
            >
              {loading && (
                <div
                  style={{
                    width: "14px",
                    height: "14px",
                    border: "2px solid rgba(255,255,255,0.3)",
                    borderTopColor: "#fff",
                    borderRadius: "50%",
                    animation: "spin 0.7s linear infinite",
                  }}
                />
              )}
              {loading ? "Creating account..." : "Create account"}
            </button>
          </form>
        </div>

        <p
          style={{
            textAlign: "center",
            fontSize: "13px",
            color: "#475569",
            marginTop: "20px",
          }}
        >
          Already have an account?{" "}
          <Link
            to="/login"
            style={{
              color: "#818cf8",
              fontWeight: 500,
              textDecoration: "none",
            }}
          >
            Sign in
          </Link>
        </p>
      </div>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
        * { font-family: 'Inter', system-ui, sans-serif; }
        @keyframes spin { to { transform: rotate(360deg); } }
        input::placeholder { color: #475569; }
      `}</style>
    </div>
  );
}
