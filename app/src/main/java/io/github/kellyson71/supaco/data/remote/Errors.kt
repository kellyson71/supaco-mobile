package io.github.kellyson71.supaco.data.remote

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Conta sem boletim (servidor, prestador de serviço). */
class NotStudentException : Exception("Conta não é de aluno")

/** Mensagem legível para o usuário — nunca mostra exceção crua. */
fun Throwable.userMessage(isLogin: Boolean = false): String = when (this) {
    is NotStudentException ->
        "O Supaco é feito para alunos. Essa conta do SUAP não tem boletim."
    is HttpException -> when (code()) {
        400, 401 -> if (isLogin) "Matrícula ou senha incorretas." else "Sua sessão expirou. Entre de novo."
        403 -> "O SUAP negou acesso a esses dados."
        404 -> "O SUAP não encontrou esses dados."
        429 -> "Muitas tentativas seguidas. Espere um pouco e tente de novo."
        in 500..599 -> "O SUAP está fora do ar ou instável agora. Tente mais tarde."
        else -> "O SUAP respondeu com erro (${code()})."
    }
    is SocketTimeoutException -> "O SUAP demorou demais para responder. Tente de novo."
    is UnknownHostException -> "Sem conexão com a internet."
    is IOException -> "Falha de conexão com o SUAP. Verifique sua internet."
    is SerializationException, is IllegalArgumentException ->
        "O SUAP respondeu num formato inesperado. Atualize o app ou tente mais tarde."
    else -> "Algo deu errado. Tente de novo."
}
