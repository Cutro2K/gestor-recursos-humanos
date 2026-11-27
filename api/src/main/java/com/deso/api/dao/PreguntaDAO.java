package com.deso.api.dao;

import com.deso.api.model.entity.Pregunta;

import java.util.List;

public interface PreguntaDAO {
    List<Pregunta> buscarActivasPorCompetencias(List<Long> competenciaIds);
}
