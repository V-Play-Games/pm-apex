/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vpg.apex

import net.vpg.apex.core.ApexTrack
import net.vpg.apex.core.Resources
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder

class ApexWindow(private val apex: Apex) : JFrame() {
    val trackName: JLabel
    val searchTextArea: JTextArea
    val categories: JComboBox<String>
    val next: JButton
    val previous: JButton
    val shuffleButton: JButton
    val playPause: JButton
    val search: JButton
    val trackList: JList<String>
    val trackListPane: JScrollPane
    val progress: JLabel
    val seekBar: JSlider

    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        trackName = JLabel("Double-click on a track to get started!")
        trackName.setToolTipText("Track Name")

        searchTextArea = JTextArea(1, 10).apply {
            addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    if (e.getKeyChar() == '\n') apex.takeAction(Apex.Action.SEARCH)
                }
            })
            alignmentX = 0f
            border = EmptyBorder(5, 5, 0, 5)
            isEditable = true
            lineWrap = false
            font = JLabel().getFont()
            isFocusable = true
            rows = 0
        }

        next = createButton("Next Track", "Go to the next track", Apex.Action.NEXT, false)
        previous = createButton("Previous Track", "Go to the previous track", Apex.Action.PREVIOUS, false)
        shuffleButton = createButton("Shuffle OFF", "Shuffle the playlist", Apex.Action.SHUFFLE, true)
        playPause = createButton("Play", "Play the track", Apex.Action.PLAY_PAUSE, false)
        search = createButton("Search", "Search a track", Apex.Action.SEARCH, true)

        val categoriesList = ApexTrack.entries.values
            .map { it.category }
            .distinct()
            .sorted()
            .toMutableList()
        categoriesList.addFirst("-- Select a category --")
        categories = JComboBox(categoriesList.toTypedArray())
        categories.addItemListener { apex.takeAction(Apex.Action.UPDATE_CATEGORY) }

        trackList = JList<String>().apply {
            visibleRowCount = 7
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        apex.takeAction(Apex.Action.CLICK_ON_PLAYLIST)
                    }
                }
            })
            addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    if (e.getKeyChar() == '\n') {
                        apex.takeAction(Apex.Action.CLICK_ON_PLAYLIST)
                    }
                }
            })
        }
        trackListPane = JScrollPane(trackList)

        seekBar = JSlider(SwingConstants.HORIZONTAL, 0, 10000, 0).apply {
            addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent?) {
                    apex.takeAction(Apex.Action.PROGRESS_SEEK)
                }
            })
            isEnabled = false
        }
        progress = JLabel("--:--/--:--")

        createMainFrame()
    }

    private fun createMainFrame() {
        title = ("PM APEX")
        defaultCloseOperation = (EXIT_ON_CLOSE)
        isResizable = false
        preferredSize = Dimension(500, 350)
        iconImage = ImageIO.read(Resources["icon.png"])
        setLocationRelativeTo(null)
        add(JTabbedPane().apply {
            add(createPlayerPanel())
            add(createCreditsPanel())
        })
        createBox(
            "South",
            createPanel(searchTextArea, search),
            Box.createVerticalStrut(5),
            createPanel(shuffleButton, previous, playPause, next)
        )
        pack()
    }

    private fun createPlayerPanel() = createPanel(
        "Player",
        categories,
        Box.createVerticalStrut(5),
        trackListPane,
        Box.createVerticalStrut(10)
    ).apply {
        createBox("Center", trackName)
        createBox("West", progress)
        createBox("South", seekBar)
    }

    private fun createCreditsPanel() = createPanel(
        "Credits and Info",
        JLabel("Welcome to Pokemon Masters Audio Player EX, PM APEX in short."),
        Box.createVerticalStrut(10),
        JLabel(
            """
                    This is an application made for playing audio tracks from Pokemon Masters.
                    It also has looping support, so go loop your favourite battle theme for as long as you want!
                    Although you can't download tracks right now, you can play them online!
                    Have Fun!
                    
                    """.trimIndent()
        ),
        Box.createVerticalStrut(10),
        JLabel("Credits").apply {
            setFont(Font(Font.SANS_SERIF, Font.BOLD, 14))
        },
        Box.createVerticalStrut(5),
        JLabel("V Play Games - The Developer of this project"),
        Box.createVerticalStrut(3),
        JLabel("Made with Java, Built with Maven 3")
    )

    private fun createPanel(name: String, vararg components: Component) = JPanel().apply {
        border = EmptyBorder(15, 15, 0, 15)
        layout = BorderLayout()
        this.name = name
        createBox("North", *components)
    }

    private fun createPanel(vararg components: Component) = JPanel().apply {
        layout = FlowLayout(FlowLayout.CENTER)
        components.forEach(::add)
    }

    private fun createButton(name: String, toolTip: String, action: Int, enabled: Boolean) = JButton(name).apply {
        toolTipText = toolTip
        isEnabled = enabled
        addActionListener { apex.takeAction(action) }
    }

    private fun Container.createBox(constraints: String, vararg components: Component) = Box.createVerticalBox().apply {
        this@createBox.add(this, constraints)
        components.forEach(this::add)
    }
}
