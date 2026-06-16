package com.example.consultacep

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.Volley
import com.example.consultacep.databinding.ActivityMainBinding

/**
 * Tela principal do app.
 *
 * Responsabilidades:
 *  - Capturar o CEP digitado pelo usuario
 *  - Validar a entrada (campo vazio / formato)
 *  - Acionar a consulta via CepService
 *  - Obter a localizacao do usuario (mediante permissao) e preencher o CEP
 *  - Exibir o resultado ou uma mensagem de erro amigavel
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cepService: CepService
    private lateinit var locationService: LocationService
    private lateinit var geocodingService: GeocodingService

    /**
     * Registra o pedido de permissao de localizacao.
     * O resultado (concedida ou negada) chega neste callback.
     */
    private val pedirPermissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            // Usuario concedeu: segue o fluxo de localizacao
            usarLocalizacao()
        } else {
            // Usuario negou: app continua funcionando, apenas avisa
            exibirCarregando(false)
            exibirMensagem(getString(R.string.permissao_negada))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filaRequisicoes = Volley.newRequestQueue(this)
        cepService = CepService(filaRequisicoes)
        geocodingService = GeocodingService(filaRequisicoes)
        locationService = LocationService(this)

        binding.btnConsultar.setOnClickListener { realizarConsulta() }
        binding.btnLocalizacao.setOnClickListener { aoTocarLocalizacao() }
    }

    // ───────────────────────────── Consulta por CEP ─────────────────────────

    /**
     * Le o CEP digitado, valida e dispara a consulta.
     */
    private fun realizarConsulta() {
        val cep = binding.etCep.text.toString().trim()

        // Validacao: campo vazio
        if (cep.isEmpty()) {
            binding.tilCep.error = getString(R.string.erro_campo_vazio)
            return
        }

        // Validacao: formato (a ViaCEP exige exatamente 8 digitos)
        if (cep.length != 8) {
            binding.tilCep.error = getString(R.string.erro_cep_invalido)
            return
        }

        binding.tilCep.error = null
        ocultarResultado()
        ocultarMensagem()
        exibirCarregando(true)

        cepService.consultar(
            cep = cep,
            aoSucesso = { endereco ->
                exibirCarregando(false)
                exibirResultado(endereco)
            },
            aoFalhar = { codigoErro ->
                exibirCarregando(false)
                val mensagem = when (codigoErro) {
                    "CEP_NAO_ENCONTRADO" -> getString(R.string.erro_cep_nao_encontrado)
                    else -> getString(R.string.erro_conexao)
                }
                exibirMensagem(mensagem)
            }
        )
    }

    // ──────────────────────────── Fluxo de permissao ────────────────────────

    /**
     * Chamado quando o usuario toca em "Usar minha localizacao".
     * Verifica se a permissao ja foi concedida:
     *  - Se sim, usa a localizacao direto.
     *  - Se nao, solicita a permissao ao usuario.
     */
    private fun aoTocarLocalizacao() {
        ocultarResultado()
        ocultarMensagem()

        val temPermissao = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (temPermissao) {
            // Cenario 1: permissao ja concedida anteriormente
            usarLocalizacao()
        } else {
            // Cenario 2: ainda nao tem permissao -> solicita ao usuario.
            // Mostra antes uma breve explicacao do porque precisamos da permissao.
            exibirMensagem(getString(R.string.permissao_explicacao))
            pedirPermissaoLocalizacao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Executado quando ja temos permissao: obtem a localizacao,
     * converte em CEP e preenche o campo automaticamente.
     */
    private fun usarLocalizacao() {
        ocultarMensagem()
        exibirCarregando(true)

        locationService.obterLocalizacaoAtual(
            aoSucesso = { latitude, longitude ->
                geocodingService.cepPorCoordenadas(
                    latitude = latitude,
                    longitude = longitude,
                    aoSucesso = { cep ->
                        exibirCarregando(false)
                        binding.etCep.setText(cep)
                        exibirMensagem(getString(R.string.cep_preenchido))
                    },
                    aoFalhar = {
                        exibirCarregando(false)
                        exibirMensagem(getString(R.string.erro_cep_localizacao))
                    }
                )
            },
            aoFalhar = {
                exibirCarregando(false)
                exibirMensagem(getString(R.string.erro_localizacao))
            }
        )
    }

    // ──────────────────────────── Exibicao na tela ──────────────────────────

    /**
     * Preenche o card com os dados retornados e o torna visivel.
     */
    private fun exibirResultado(endereco: Endereco) {
        binding.tvLogradouro.text = ouTraco(endereco.logradouro)
        binding.tvBairro.text = ouTraco(endereco.bairro)
        binding.tvCidade.text = ouTraco(endereco.localidade)
        binding.tvEstado.text = ouTraco(endereco.uf)
        binding.tvDdd.text = ouTraco(endereco.ddd)
        binding.cardResultado.visibility = View.VISIBLE
    }

    /**
     * Alguns CEPs (ex.: de cidades pequenas) retornam logradouro vazio.
     * Nesses casos mostramos um traco em vez de um campo em branco.
     */
    private fun ouTraco(valor: String): String {
        return if (valor.isBlank()) "—" else valor
    }

    private fun exibirCarregando(carregando: Boolean) {
        binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
        binding.btnConsultar.isEnabled = !carregando
        binding.btnLocalizacao.isEnabled = !carregando
    }

    private fun exibirMensagem(texto: String) {
        binding.tvMensagem.text = texto
        binding.tvMensagem.visibility = View.VISIBLE
    }

    private fun ocultarMensagem() {
        binding.tvMensagem.visibility = View.GONE
    }

    private fun ocultarResultado() {
        binding.cardResultado.visibility = View.GONE
    }
}
