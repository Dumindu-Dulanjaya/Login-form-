import { useState } from "react";
import Login from "./components/Login.jsx";
import Register from "./components/Register.jsx";

function App() {
  const [showLogin, setShowLogin] = useState(true);

  return (
    <>
      {showLogin ? (
        <Login onSwitchToRegister={() => setShowLogin(false)} />
      ) : (
        <Register onSwitchToLogin={() => setShowLogin(true)} />
      )}
    </>
  );
}

export default App;
