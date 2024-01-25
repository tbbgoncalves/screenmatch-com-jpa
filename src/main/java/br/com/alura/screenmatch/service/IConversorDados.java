package br.com.alura.screenmatch.service;

public interface IConversorDados {
    <T> T pegarDados(String json, Class<T> classType);
}
