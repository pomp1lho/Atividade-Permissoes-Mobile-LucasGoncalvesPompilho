package com.example.consultacep

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

/**
 * Responsavel pela geocodificacao reversa: converte coordenadas
 * (latitude e longitude) em um CEP, usando a API publica Nominatim
 * do OpenStreetMap.
 *
 * Assim como o CepService, toda a logica de rede fica isolada aqui.
 */
class GeocodingService(private val filaRequisicoes: RequestQueue) {

    /**
     * Converte coordenadas em um CEP (apenas numeros).
     *
     * @param latitude latitude obtida do GPS
     * @param longitude longitude obtida do GPS
     * @param aoSucesso callback com o CEP encontrado (8 digitos)
     * @param aoFalhar callback com mensagem de erro
     */
    fun cepPorCoordenadas(
        latitude: Double,
        longitude: Double,
        aoSucesso: (String) -> Unit,
        aoFalhar: (String) -> Unit
    ) {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$latitude&lon=$longitude&format=json&addressdetails=1"

        val requisicao = object : JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { resposta ->
                val cep = extrairCep(resposta)
                if (cep != null) {
                    aoSucesso(cep)
                } else {
                    aoFalhar("CEP_NAO_ENCONTRADO")
                }
            },
            {
                aoFalhar("ERRO_CONEXAO")
            }
        ) {
            // A Nominatim exige um User-Agent identificando o app
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "ConsultaCEP-App/1.0"
                return headers
            }
        }

        filaRequisicoes.add(requisicao)
    }

    /**
     * Extrai o CEP do JSON da Nominatim, removendo o hifen para
     * ficar no formato de 8 digitos aceito pela ViaCEP.
     * Retorna null se nao houver CEP valido na resposta.
     */
    private fun extrairCep(json: JSONObject): String? {
        val endereco = json.optJSONObject("address") ?: return null
        val postcode = endereco.optString("postcode")
        val somenteNumeros = postcode.replace("-", "").replace(" ", "")
        return if (somenteNumeros.length == 8) somenteNumeros else null
    }
}
