package com.universidad.catalogo.repository;

import com.universidad.catalogo.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

// Prueba de integración: carga solo la capa de persistencia (entidades,
// repositorios y H2 en memoria). Cada test corre en una transacción que se
// revierte al terminar, por eso setUp() puede insertar los mismos datos siempre.
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired ProductoRepository repo;
    @Autowired TestEntityManager em;

    @BeforeEach
    void setUp() {
        em.persistAndFlush(crear("Laptop", 1500.0, "ELECTRONICA"));
        em.persistAndFlush(crear("Cuaderno", 5.0, "PAPELERIA"));
    }

    @Test
    void findByCategoria_electronica_retornaUnProducto() {
        assertThat(repo.findByCategoria("ELECTRONICA")).hasSize(1)
            .extracting("nombre").containsExactly("Laptop");
    }

    @Test
    void findByCategoria_sinCoincidencias_retornaListaVacia() {
        assertThat(repo.findByCategoria("JUGUETERIA")).isEmpty();
    }

    @Test
    void save_productoNuevo_asignaIdYSePuedeRecuperar() {
        Producto guardado = repo.save(crear("Mouse", 25.0, "ELECTRONICA"));
        em.clear(); // fuerza a leer de la base de datos y no del contexto en memoria
        assertThat(guardado.getId()).isNotNull();
        assertThat(repo.findById(guardado.getId())).get()
            .extracting(Producto::getNombre).isEqualTo("Mouse");
    }

    private Producto crear(String nombre, double precio, String categoria) {
        Producto p = new Producto();
        p.setNombre(nombre); p.setPrecio(precio); p.setCategoria(categoria);
        return p;
    }
}
