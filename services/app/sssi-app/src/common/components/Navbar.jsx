export function Navbar() {
  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <h2>SSSI</h2>
      </div>
      <ul className="navbar-menu">
        <li><a href="/">Home</a></li>
        <li><a href="/auth">Auth</a></li>
        <li><a href="/inventory">Inventory</a></li>
        <li><a href="/security">Security</a></li>
      </ul>
    </nav>
  );
}

export default Navbar;
