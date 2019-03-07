package io.github.saltyJeff.launchpad.telem

import com.digi.xbee.api.XBeeDevice
import com.sun.org.apache.xpath.internal.operations.Bool
import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.TYPE_DICT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private val table: JTable = JTable(1, Telemetry.fields.size)
    private val tableModel: DefaultTableModel
    private val timer = Timer(32, this)
    init {
        tableModel = table.model as DefaultTableModel
        tableModel.setColumnIdentifiers(Telemetry.fields.map { it.fieldName }.toTypedArray())
        add(table)
        pack()
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
        dataHolder.withIndex().forEach {
            tableModel.setValueAt(it.value, 0, it.index)
        }
        tableModel.fireTableRowsUpdated(0, 0)
    }
}