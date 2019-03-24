package io.github.saltyJeff.launchpad.telem

import com.digi.xbee.api.XBeeDevice
import com.sun.org.apache.xpath.internal.operations.Bool
import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.TYPE_DICT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel


class StreamFrame(private val dataHolder: MutableList<String>): JFrame(), ActionListener {
    private val timer = Timer(32, this)
    private val dataLabels = mutableListOf<JLabel>()
    private val boldFont = Font("Header", Font.BOLD, 16)
    private val dataFont = Font("Data", Font.PLAIN, 16)
    init {
        pack()
        title = "Rocket Telemetry"
        defaultCloseOperation = 0
        isResizable = false
    }

    fun setFields(fields: List<CField>) {
        contentPane.removeAll()
        val layout = GridLayout(fields.size,2, 5, 10)
        this.layout = layout
        fields.forEach {
            val fieldLabel = JLabel(" ${it.fieldName}")
            fieldLabel.font = boldFont
            add(fieldLabel)
            val dataLabel = JLabel("undef", SwingConstants.RIGHT)
            dataLabel.font = dataFont
            dataLabels.add(dataLabel)
            add(dataLabel)
        }
        pack()
        size = Dimension(400, height)
    }

    override fun setVisible(b: Boolean) {
        super.setVisible(b)
        if(b) {
            timer.start()
        }
        else {
            timer.stop()
        }
    }
    override fun actionPerformed(e: ActionEvent?) {
        dataLabels.withIndex().forEach {
            it.value.text = "${dataHolder[it.index]} "
        }
    }
}