export function Navbar() {
  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <h2>SSSI</h2>
      </div>
      <ul className="navbar-menu">
        <li><a href="/">Home</a></li>
        <li><a href="/auth">Auth</a></li>
        <li><a href="/inventario">Inventario</a></li>
        <li><a href="/seguridad/usuarios">Seguridad</a></li>
      </ul>
    </nav>
  );
}

export default Navbar;
