package com.reservar.service;

import com.reservar.dto.servicio.CategoriaCatalogoResponse;
import com.reservar.dto.servicio.TipoServicioCatalogoResponse;
import com.reservar.entity.CategoriaServicio;
import com.reservar.entity.TipoServicio;
import com.reservar.repository.CategoriaServicioRepository;
import com.reservar.repository.TipoServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogoServicioService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private final CategoriaServicioRepository categoriaRepository;
    private final TipoServicioRepository tipoRepository;

    public CatalogoServicioService(CategoriaServicioRepository categoriaRepository, TipoServicioRepository tipoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.tipoRepository = tipoRepository;
    }

    public List<CategoriaCatalogoResponse> listarCategorias() {
        return categoriaRepository.findByEstado(ESTADO_ACTIVO)
                .stream()
                .map(this::convertirCategoria)
                .toList();
    }

    public List<TipoServicioCatalogoResponse> listarTipos() {
        return tipoRepository.findByEstado(ESTADO_ACTIVO)
                .stream()
                .map(this::convertirTipo)
                .toList();
    }

    public List<TipoServicioCatalogoResponse> listarTiposPorCategoria(Long idCategoria) {
        return tipoRepository
                .findByCategoriaIdCategoriaAndEstado(idCategoria, ESTADO_ACTIVO)
                .stream()
                .map(this::convertirTipo)
                .toList();
    }

    private CategoriaCatalogoResponse convertirCategoria(CategoriaServicio categoria) {
        return new CategoriaCatalogoResponse(
                categoria.getIdCategoria(),
                categoria.getNombre(),
                categoria.getDescripcion()
        );
    }

    private TipoServicioCatalogoResponse convertirTipo(TipoServicio tipo) {
        return new TipoServicioCatalogoResponse(
                tipo.getIdTipoServicio(),
                tipo.getCategoria().getIdCategoria(),
                tipo.getCategoria().getNombre(),
                tipo.getNombre(),
                tipo.getDescripcion()
        );
    }
}