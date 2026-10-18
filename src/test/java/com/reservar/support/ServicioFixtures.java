package com.reservar.support;

import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.OfertaServicio;
import com.reservar.entity.Proveedor;
import com.reservar.entity.TipoServicio;
import com.reservar.entity.Usuario;
import com.reservar.entity.enums.EstadoUsuario;
import com.reservar.entity.enums.Rol;

import java.util.UUID;

/** Constructores de datos de prueba compartidos por las pruebas de la Épica 2. */
public final class ServicioFixtures {

    public static final Long ID_SERVICIO = 10L;
    public static final Long ID_PROVEEDOR = 3L;
    public static final Long ID_TIPO = 2L;
    public static final Long ID_CATEGORIA = 1L;

    private ServicioFixtures() {
    }

    public static Usuario usuario(Rol rol, EstadoUsuario estado) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(UUID.randomUUID());
        usuario.setNombreCompleto("Ana Proveedora");
        usuario.setRol(rol);
        usuario.setEstado(estado);
        return usuario;
    }

    public static Usuario proveedorActivo() {
        return usuario(Rol.PROVEEDOR, EstadoUsuario.ACTIVO);
    }

    public static Proveedor proveedor(Long id, Usuario usuario) {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(id);
        proveedor.setUsuario(usuario);
        return proveedor;
    }

    public static CategoriaServicio categoria(String estado) {
        CategoriaServicio categoria = new CategoriaServicio();
        categoria.setIdCategoria(ID_CATEGORIA);
        categoria.setNombre("SALUD");
        categoria.setDescripcion("Servicios de salud");
        categoria.setEstado(estado);
        return categoria;
    }

    public static TipoServicio tipo(String estadoTipo, CategoriaServicio categoria) {
        TipoServicio tipo = new TipoServicio();
        tipo.setIdTipoServicio(ID_TIPO);
        tipo.setNombre("Odontologia");
        tipo.setDescripcion("Cuidado dental");
        tipo.setEstado(estadoTipo);
        tipo.setCategoria(categoria);
        return tipo;
    }

    public static TipoServicio tipoActivo() {
        return tipo("ACTIVO", categoria("ACTIVO"));
    }

    public static OfertaServicio oferta(Proveedor proveedor, TipoServicio tipo, String estado) {
        OfertaServicio oferta = new OfertaServicio();
        oferta.setIdServicio(ID_SERVICIO);
        oferta.setProveedor(proveedor);
        oferta.setTipoServicio(tipo);
        oferta.setNombre("Limpieza dental");
        oferta.setDescripcion("Limpieza completa");
        oferta.setDuracionMinutos(45);
        oferta.setModalidad("PRESENCIAL");
        oferta.setUbicacion("Villavicencio");
        oferta.setEstado(estado);
        return oferta;
    }
}
