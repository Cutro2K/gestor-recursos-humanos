package com.deso.api.dao;

import com.deso.api.model.entity.ConfiguracionCuestionario;

import java.util.Optional;

public interface ConfiguracionCuestionarioDAO {
    Optional<ConfiguracionCuestionario> buscarVigente();
}
