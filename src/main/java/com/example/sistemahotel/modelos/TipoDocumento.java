package com.example.sistemahotel.modelos;

public enum TipoDocumento {
    DNI("DNI"),
    PASAPORTE("Pasaporte"),
    CARNE_EXTRANJERIA("Carné de extranjería");

    private final String etiqueta;

    TipoDocumento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
