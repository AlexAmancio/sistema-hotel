package com.example.sistemahotel.interfaces;

import java.util.List;

import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Registro;

public interface IConsumoService {
    List<Consumo> listarPorRegistro(int registroId);
    void agregar(Registro registro, String descripcion, double monto);
    // Devuelve el id del registro al que pertenecía, para poder redirigir de vuelta.
    int eliminar(int consumoId);
}
