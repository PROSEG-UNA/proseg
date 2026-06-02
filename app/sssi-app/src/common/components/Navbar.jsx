export function Navbar() {
  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <h2>SPSG</h2>
      </div>
      <ul className="navbar-menu">
        <li><a href="/">Home</a></li>
        <li><a href="/auth">Auth</a></li>
        <li><a href="/home">Inventario</a></li>
        <li><a href="/mantenimiento/tickets">Tickets</a></li>
        <li><a href="/seguridad/usuarios">Seguridad</a></li>
      </ul>
    </nav>
  );
}

export default Navbar;
