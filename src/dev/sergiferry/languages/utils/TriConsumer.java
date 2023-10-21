package dev.sergiferry.languages.utils;

/**
 * Creado por SergiFerry el 09/10/2023
 */
public interface TriConsumer<K, V, S> {
    void accept(K k, V v, S s);
}
