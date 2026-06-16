package com.example.consultacep

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Responsavel por obter a localizacao atual do dispositivo usando
 * o FusedLocationProviderClient do Google Play Services.
 *
 * Esta classe NAO lida com permissoes - ela assume que a permissao
 * ja foi concedida antes de ser chamada. Quem controla a permissao
 * e a MainActivity.
 */
class LocationService(context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Obtem a localizacao atual do dispositivo.
     *
     * @param aoSucesso callback com latitude e longitude
     * @param aoFalhar callback chamado quando a localizacao nao pode ser obtida
     *
     * A anotacao @SuppressLint e necessaria porque a verificacao de permissao
     * e feita na MainActivity antes desta chamada.
     */
    @SuppressLint("MissingPermission")
    fun obterLocalizacaoAtual(
        aoSucesso: (latitude: Double, longitude: Double) -> Unit,
        aoFalhar: () -> Unit
    ) {
        val cancellationToken = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                aoSucesso(location.latitude, location.longitude)
            } else {
                aoFalhar()
            }
        }.addOnFailureListener {
            aoFalhar()
        }
    }
}
