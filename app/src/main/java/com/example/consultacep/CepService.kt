package com.example.consultacep

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

/**
 * Responsavel por consultar a API publica ViaCEP e converter
 * a resposta JSON em um objeto Endereco.
 *
 * Toda a logica de rede fica isolada aqui, separada da interface (MainActivity).
 */
class CepService(private val filaRequisicoes: RequestQueue) {

    /**
     * Consulta um CEP na API ViaCEP.
     *
     * @param cep CEP com 8 digitos (somente numeros)
     * @param aoSucesso callback chamado com o Endereco quando a consulta funciona
     * @param aoFalhar callback chamado com uma mensagem de erro quando algo da errado
     */
    fun consultar(
        cep: String,
        aoSucesso: (Endereco) -> Unit,
        aoFalhar: (String) -> Unit
    ) {
        val url = "https://viacep.com.br/ws/$cep/json/"

        val requisicao = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { resposta ->
                // A ViaCEP retorna {"erro": true} quando o CEP nao existe
                if (resposta.has("erro")) {
                    aoFalhar("CEP_NAO_ENCONTRADO")
                } else {
                    aoSucesso(converterParaEndereco(resposta))
                }
            },
            {
                // Falha de conexao, timeout ou resposta invalida
                aoFalhar("ERRO_CONEXAO")
            }
        )

        filaRequisicoes.add(requisicao)
    }

    /**
     * Converte o JSON da ViaCEP em um objeto Endereco,
     * usando optString para evitar erros caso algum campo venha ausente.
     */
    private fun converterParaEndereco(json: JSONObject): Endereco {
        return Endereco(
            cep = json.optString("cep"),
            logradouro = json.optString("logradouro"),
            bairro = json.optString("bairro"),
            localidade = json.optString("localidade"),
            uf = json.optString("uf"),
            ddd = json.optString("ddd")
        )
    }
}
