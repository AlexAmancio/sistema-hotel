package com.example.sistemahotel.aceptacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import io.cucumber.java.After;
import io.cucumber.java.Before;

// Abre una transacción antes de cada escenario y la deshace al final. MockMvc
// atiende la petición en el mismo hilo, así que los controladores y servicios
// participan de esa transacción y nada queda guardado en la base.
public class Hooks {

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionStatus transaccion;

    @Before
    public void abrirTransaccion() {
        transaccion = transactionManager.getTransaction(new DefaultTransactionDefinition());
    }

    @After
    public void deshacerTransaccion() {
        if (transaccion != null && !transaccion.isCompleted()) {
            transactionManager.rollback(transaccion);
        }
    }
}
