package pe.upeu.ecorecicla

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.domain.model.Sesion

class AlmacenSeguroAndroid(context: Context) : AlmacenSeguro {
    private val prefs=context.getSharedPreferences("sesion_cifrada",Context.MODE_PRIVATE)
    private val alias="EcoRutasSesion"
    private fun clave(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun guardar(sesion: Sesion,expiresIn: Long) {
        val json=JSONObject().put("token",sesion.token).put("usuario",sesion.usuario).put("nombre",sesion.nombre).put("id",sesion.id).put("rol",sesion.rol).put("privilegios",JSONArray(sesion.privilegios.toList())).put("expires",System.currentTimeMillis()+expiresIn*1000)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,clave()) }
        val encrypted=cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)).putString("datos",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit())
    }
    override fun leer(): Sesion? {
        val data=prefs.getString("datos",null) ?: return null
        return try {
            val iv=Base64.decode(prefs.getString("iv",null),Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,clave(),GCMParameterSpec(128,iv)) }
            val json=JSONObject(String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),Charsets.UTF_8))
            if(json.getLong("expires")<=System.currentTimeMillis()) { borrar(); null }
            else { val p=json.getJSONArray("privilegios"); Sesion(json.getString("token"),json.getString("usuario"),json.getString("rol"),(0 until p.length()).map { p.getString(it) }.toSet(),json.optLong("id",0),json.optString("nombre",json.getString("usuario"))) }
        } catch (_: Exception) { borrar(); null }
    }
    override fun borrar() { check(prefs.edit().clear().commit()) }
}
