package com.safefield.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClosePtActivity : Activity() {
    private lateinit var repo: PtRepository
    private lateinit var data: PtData
    private val closePhotoRequest: Int = 9101
    private val closeSignatureRequest: Int = 9102

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        repo = PtRepository(this)
        data = repo.load()
        if (data.closedBy.isBlank()) data.closedBy = data.closureResponsible.ifBlank { data.responsible }
        render()
    }

    override fun onPause(): Unit {
        super.onPause()
        repo.save(data)
    }

    override fun onBackPressed(): Unit {
        repo.save(data)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?): Unit {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == closeSignatureRequest) {
            if (resultCode == RESULT_OK) {
                val signature = resultData?.getStringExtra(SignatureActivity.EXTRA_SIGNATURE_B64).orEmpty()
                if (signature.isNotBlank()) {
                    data.closeSignatureB64 = signature
                    data.closureSignatureB64 = signature
                    repo.save(data)
                    Toast.makeText(this, "Assinatura de encerramento salva", Toast.LENGTH_SHORT).show()
                }
            }
            render()
            return
        }
        if (requestCode == closePhotoRequest && resultCode == RESULT_OK) {
            val clip = resultData?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) addClosePhoto(clip.getItemAt(i).uri)
            } else {
                resultData?.data?.let { addClosePhoto(it) }
            }
            repo.save(data)
            render()
        }
    }

    private fun addClosePhoto(uri: Uri): Unit {
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        data.closePhotoUris.add(uri.toString())
        data.closurePhotoUris = data.closePhotoUris.toMutableList()
    }

    private fun render(): Unit {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Ui.SHELL)
        val content = Ui.vbox(this, Ui.dp(this, 20))
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val header = Ui.row(this)
        val back = Ui.ghostButton(this, "‹")
        back.textSize = 24f
        back.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48))
        back.setOnClickListener { repo.save(data); finish() }
        header.addView(back)
        val title = Ui.vbox(this)
        title.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        title.addView(Ui.title(this, "Encerrar PT", 25f))
        title.addView(Ui.label(this, PtFlowEngine.ptNumber(data)))
        header.addView(title)
        content.addView(header.margin(0, Ui.dp(this, 4)))

        val expired = data.endMillis > 0L && System.currentTimeMillis() > data.endMillis
        if (expired) content.addView(Ui.value(this, "PT vencida. Registre o encerramento com atenção.", Ui.RED).margin(0, Ui.dp(this, 6)))

        content.addView(closeDataCard().margin(0, Ui.dp(this, 8)))
        content.addView(closePhotoCard().margin(0, Ui.dp(this, 8)))
        content.addView(closeSignatureCard().margin(0, Ui.dp(this, 8)))

        closeValidationMessage()?.let { content.addView(Ui.value(this, it, Ui.RED).margin(0, Ui.dp(this, 6))) }
        val confirm = Ui.button(this, if (isClosed()) "Atualizar encerramento" else "Confirmar encerramento", if (closeValidationMessage() == null) Ui.GREEN else Ui.RED)
        confirm.setOnClickListener {
            val message = closeValidationMessage()
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show() else confirmClose()
        }
        content.addView(confirm.margin(0, Ui.dp(this, 10)))

        if (isClosed()) {
            val share = Ui.ghostButton(this, "Compartilhar PDF atualizado")
            share.setOnClickListener { shareUpdatedPdf() }
            content.addView(share.margin(0, Ui.dp(this, 4)))
        }

        Ui.animateIn(content)
        setContentView(scroll)
    }

    private fun closeDataCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.section(this, "Dados do encerramento"))
        addInput(card, "Responsável pelo encerramento", data.closedBy) { data.closedBy = it }
        addInput(card, "Data/hora de encerramento", data.closedAt) { data.closedAt = it }
        val now = Ui.ghostButton(this, "Usar data/hora atual")
        now.setOnClickListener { data.closedAt = timestamp(); repo.save(data); render() }
        card.addView(now.margin(0, Ui.dp(this, 4)))
        card.addView(conditionSelector().margin(0, Ui.dp(this, 8)))
        addInput(card, "Observações finais", data.closeNotes, true) { data.closeNotes = it }
        addInput(card, "Pendências encontradas", data.closePendingIssues, true) { data.closePendingIssues = it }
        card.addView(incidentSelector().margin(0, Ui.dp(this, 8)))
        if (data.closeHadIncident) addInput(card, "Descrição do incidente", data.closeIncidentDescription, true) { data.closeIncidentDescription = it }
        return card
    }

    private fun addInput(parent: LinearLayout, label: String, value: String, multi: Boolean = false, onChange: (String) -> Unit): Unit {
        parent.addView(Ui.label(this, label).margin(0, Ui.dp(this, 7)))
        val edit = Ui.input(this, label, multi)
        edit.setText(value)
        edit.addTextChangedListener(SimpleWatcher {
            onChange(edit.text.toString())
            repo.save(data)
        })
        parent.addView(edit.margin(0, Ui.dp(this, 4)))
    }

    private fun conditionSelector(): LinearLayout {
        val box = Ui.vbox(this)
        box.addView(Ui.label(this, "Condição final da área"))
        listOf(
            "Área limpa e liberada",
            "Isolamento removido",
            "Equipamentos retirados",
            "Atividade concluída sem pendências",
            "Atividade concluída com observações",
            "Atividade paralisada",
            "Área entregue com pendências"
        ).forEach { option ->
            val button = Ui.ghostButton(this, option)
            button.gravity = Gravity.CENTER_VERTICAL
            button.setTextColor(if (data.closeCondition == option) Ui.AMBER_SOFT else Ui.TEXT)
            button.setOnClickListener { data.closeCondition = option; repo.save(data); render() }
            box.addView(button.margin(0, Ui.dp(this, 4)))
        }
        return box
    }

    private fun incidentSelector(): LinearLayout {
        val row = Ui.row(this)
        val label = Ui.label(this, "Houve incidente?")
        label.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(label)
        val no = Ui.ghostButton(this, "Não")
        val yes = Ui.ghostButton(this, "Sim")
        no.setTextColor(if (!data.closeHadIncident) Ui.GREEN else Ui.TEXT)
        yes.setTextColor(if (data.closeHadIncident) Ui.RED else Ui.TEXT)
        no.setOnClickListener { data.closeHadIncident = false; data.closeIncidentDescription = ""; repo.save(data); render() }
        yes.setOnClickListener { data.closeHadIncident = true; repo.save(data); render() }
        row.addView(no, LinearLayout.LayoutParams(Ui.dp(this, 82), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(yes, LinearLayout.LayoutParams(Ui.dp(this, 82), ViewGroup.LayoutParams.WRAP_CONTENT))
        return row
    }

    private fun closePhotoCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.chip(this, "FOTOS FINAIS", Ui.AMBER))
        card.addView(Ui.title(this, "Evidências finais", 18f).margin(0, Ui.dp(this, 8)))
        card.addView(Ui.value(this, "Fotos finais anexadas: ${data.closePhotoUris.size}"))
        val button = Ui.button(this, "Anexar fotos finais")
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(intent, closePhotoRequest)
        }
        card.addView(button.margin(0, Ui.dp(this, 8)))
        return card
    }

    private fun closeSignatureCard(): LinearLayout {
        val card = Ui.card(this)
        val saved = data.closeSignatureB64.isNotBlank()
        card.addView(Ui.chip(this, "ASSINATURA FINAL", if (saved) Ui.GREEN else Ui.RED))
        card.addView(Ui.title(this, "Assinatura do encerramento", 18f).margin(0, Ui.dp(this, 8)))
        card.addView(Ui.value(this, if (saved) "Assinatura de encerramento salva" else "Assinatura de encerramento pendente", if (saved) Ui.GREEN else Ui.RED))
        repo.base64ToBitmap(data.closeSignatureB64)?.let {
            val preview = ImageView(this)
            preview.setImageBitmap(it)
            preview.setBackgroundColor(android.graphics.Color.WHITE)
            preview.adjustViewBounds = true
            preview.maxHeight = Ui.dp(this, 140)
            card.addView(preview.margin(0, Ui.dp(this, 8)))
        }
        val button = Ui.button(this, if (saved) "Reassinar encerramento" else "Assinar encerramento")
        button.setOnClickListener {
            val intent = Intent(this, SignatureActivity::class.java)
            intent.putExtra(SignatureActivity.EXTRA_TITLE, "Assinatura de encerramento")
            startActivityForResult(intent, closeSignatureRequest)
        }
        card.addView(button.margin(0, Ui.dp(this, 8)))
        return card
    }

    private fun closeValidationMessage(): String? {
        if (data.closedBy.isBlank()) return "Responsável pelo encerramento pendente"
        if (data.closedAt.isBlank()) return "Data/hora de encerramento pendente"
        if (data.closeCondition.isBlank()) return "Condição final da área não informada"
        if (data.closeSignatureB64.isBlank()) return "Assinatura de encerramento pendente"
        if (data.closeHadIncident && data.closeIncidentDescription.isBlank()) return "Descrição do incidente pendente"
        return null
    }

    private fun confirmClose(): Unit {
        data.closureAt = data.closedAt
        data.closureResponsible = data.closedBy
        data.closureAreaCondition = data.closeCondition
        data.closureNotes = buildString {
            append(data.closeNotes)
            if (data.closeHadIncident && data.closeIncidentDescription.isNotBlank()) {
                if (isNotBlank()) append("\n")
                append("Incidente: ").append(data.closeIncidentDescription)
            }
        }
        data.closurePending = data.closePendingIssues
        data.closureIncident = data.closeHadIncident
        data.closureSignatureB64 = data.closeSignatureB64
        data.closurePhotoUris = data.closePhotoUris.toMutableList()
        updateHistory()
        repo.save(data)
        Toast.makeText(this, "PT encerrada com sucesso", Toast.LENGTH_LONG).show()
        render()
    }

    private fun updateHistory(): Unit {
        if (data.history.isEmpty()) return
        val current = data.history.first()
        data.history[0] = current.copy(
            status = "ENCERRADA",
            closedAt = data.closedAt,
            closeNote = data.closeNotes,
            closeResponsible = data.closedBy,
            closeCondition = data.closeCondition,
            closeIncident = data.closeHadIncident,
            closeIncidentDescription = data.closeIncidentDescription,
            closePhotoCount = data.closePhotoUris.size
        )
    }

    private fun shareUpdatedPdf(): Unit {
        val file = PtPdfGenerator(this, repo).generate(data)
        val stamp = timestamp()
        data.history.add(
            0,
            PtHistoryItem(
                ptNumber = PtFlowEngine.ptNumber(data),
                emittedAt = stamp,
                place = data.place,
                company = data.company,
                responsible = data.responsible,
                fileName = file.name,
                status = "ENCERRADA",
                startMillis = data.startMillis,
                endMillis = data.endMillis,
                closedAt = data.closedAt,
                closeNote = data.closeNotes,
                closeResponsible = data.closedBy,
                closeCondition = data.closeCondition,
                closeIncident = data.closeHadIncident,
                closeIncidentDescription = data.closeIncidentDescription,
                closePhotoCount = data.closePhotoUris.size
            )
        )
        while (data.history.size > 10) data.history.removeAt(data.history.lastIndex)
        repo.save(data)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/pdf"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(share, "Compartilhar PT SafeField"))
    }

    private fun isClosed(): Boolean = data.closedAt.isNotBlank() && data.closeSignatureB64.isNotBlank()
    private fun timestamp(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
}
