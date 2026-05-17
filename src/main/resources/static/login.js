function login() {
  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value.trim();
  const msg = document.getElementById("msg");

  msg.innerText = "";

  if (!username || !password) {
    msg.innerText = "Username and password required";
    return;
  }

  fetch("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      username: username,
      password: password
    })
  })
  .then(res => {
    if (!res.ok) throw new Error("Invalid credentials");
    return res.text(); // JWT token
  })
  .then(token => {
    localStorage.setItem("token", token);
    window.location.href = "dashboard.html";
  })
  .catch(() => {
    msg.innerText = "Invalid username or password";
  });
}