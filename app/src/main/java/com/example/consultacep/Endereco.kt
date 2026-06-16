package com.example.consultacep

/**
 * Representa os dados de endereco retornados pela API ViaCEP.
 * Apenas os campos efetivamente exibidos no app sao mapeados aqui.
 */
data class Endereco(
    val cep: String,
    val logradouro: String,
    val bairro: String,
    val localidade: String,
    val uf: String,
    val ddd: String
)
