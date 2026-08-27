package com.example.sistemahotel.interfaces;

import java.util.Optional;

import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Registro;

public interface IComprobanteService {
    Optional<Comprobante> consultarPorRegistro(int registroId);
    Comprobante emitir(Registro registro);
}
