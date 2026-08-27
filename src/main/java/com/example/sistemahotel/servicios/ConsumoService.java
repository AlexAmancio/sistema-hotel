package com.example.sistemahotel.servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IConsumoRepository;

@Service
public class ConsumoService implements IConsumoService {

    @Autowired
    private IConsumoRepository repositorio;

    @Override
    public List<Consumo> listarPorRegistro(int registroId) {
        return repositorio.findByRegistroId(registroId);
    }

    @Override
    public void agregar(Registro registro, String descripcion, double monto) {
        Consumo consumo = new Consumo();
        consumo.setRegistro(registro);
        consumo.setDescripcion(descripcion);
        consumo.setMonto(monto);
        repositorio.save(consumo);
    }

    @Override
    public int eliminar(int consumoId) {
        Consumo consumo = repositorio.findById(consumoId).orElse(null);
        int registroId = (consumo != null && consumo.getRegistro() != null) ? consumo.getRegistro().getId() : 0;
        repositorio.deleteById(consumoId);
        return registroId;
    }
}
