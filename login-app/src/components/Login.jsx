import { useState } from "react";
import "./Login.css";

export default function Login({ onSwitchToRegister }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [nicNumber, setNicNumber] = useState("");
  const [nicError, setNicError] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const validateNIC = (nic) => {
    const trimmedNIC = nic.trim();
    
    if (!trimmedNIC) {
      return "NIC Number is required";
    }

    if (/[^a-zA-Z0-9]/.test(trimmedNIC)) {
      return "NIC cannot contain symbols or special characters";
    }

    const newNICPattern = /^\d{12}$/;
    const oldNICPattern = /^\d{9}[a-zA-Z]$/;

    if (newNICPattern.test(trimmedNIC)) {
      return "";
    } else if (oldNICPattern.test(trimmedNIC)) {
      return "";
    } else {
      if (/^\d+$/.test(trimmedNIC)) {
        if (trimmedNIC.length < 12) {
          return "New NIC format must contain exactly 12 digits";
        } else if (trimmedNIC.length > 12) {
          return "New NIC format must contain exactly 12 digits";
        }
      } else if (/^\d{9}/.test(trimmedNIC)) {
        return "Old NIC format must be 9 digits followed by 1 letter (e.g., 123456789V)";
      } else {
        return "Invalid NIC format. Use 12 digits (new) or 9 digits + 1 letter (old)";
      }
    }
  };

  const handleNICChange = (e) => {
    const value = e.target.value;
    setNicNumber(value);
    
    const validationError = validateNIC(value);
    setNicError(validationError);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    
    const nicValidationError = validateNIC(nicNumber);
    if (nicValidationError) {
      setNicError(nicValidationError);
      return;
    }

    setLoading(true);

    try {
      console.log("Sending login request:", { username, password, nicNumber });
      
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password, nicNumber }),
      });

      console.log("Response status:", response.status);
      const data = await response.json();
      console.log("Response data:", data);

      if (response.ok) {
        localStorage.setItem("token", data.token);
        alert("Login successful!");
      } else {
        setError(data.message || "Login failed");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-box">
        <h2>Login</h2>
        <p className="subtitle">Please sign in to continue</p>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <input
              type="text"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="input-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <div className="input-group">
            <input
              type="text"
              placeholder="NIC Number"
              value={nicNumber}
              onChange={handleNICChange}
              required
              className={nicError ? "input-error" : ""}
            />
          </div>
          {nicError && <div className="validation-error">{nicError}</div>}

          <a href="#" className="forgot-link">
            Forgot password?
          </a>

          <button type="submit" disabled={loading || nicError}>
            {loading ? "Loading..." : "Login"}
          </button>

          <p className="register-text">
            Don't have an account?{" "}
            <a href="#" onClick={(e) => { e.preventDefault(); if (onSwitchToRegister) onSwitchToRegister(); }}>
              Register
            </a>
          </p>
        </form>
      </div>
    </div>
  );
}
