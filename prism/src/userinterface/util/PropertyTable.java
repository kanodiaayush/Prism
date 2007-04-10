//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package userinterface.util;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.text.*;

/**
 *
 * @author  ug60axh
 */
public class PropertyTable extends JPanel implements ListSelectionListener, TableModelListener, ItemListener
{
    private PropertyTableModel theModel;
    
    private int lineWidth;
    /** Creates new form PropertyTable */
    public PropertyTable(PropertyTableModel theModel)
    {
        super();
        
        this.theModel = theModel;
        initComponents();

		theModel.setJTable(theTable);
        theModel.addTableModelListener(this);
        lineWidth = theTable.getRowHeight();
        theTable.setModel(theModel);
        theTable.setRowSelectionAllowed(true);
        theTable.setColumnSelectionAllowed(false);
        theTable.setCellSelectionEnabled(false);
        theTable.getSelectionModel().addListSelectionListener(this);
        theTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        theCombo.setModel(theModel.getComboModel());
        theCombo.addItemListener(this);
        
		
        theTable.getColumnModel().getColumn(0).setMinWidth(30);
		//theTable.getColumnModel().getColumn(0).setResizable(false);
        
        //int lines = 2;
        //theTable.setRowHeight( theTable.getRowHeight() * lines);
        
        TableColumn column = theTable.getColumnModel().getColumn(1);
        column.setCellRenderer(new PropertyCellRenderer());
        setCurrEditor(new PropertyTable.PropertyCellEditor());
        
        
        for(int i = 0; i < theModel.getRowCount(); i++)
        {
            ////System.out.println("Row "+i);
            String value = theModel.getValueAt(i, 1).toString();
            int lines = 1;
            
            if(theModel.getValueAt(i, 1) instanceof SingleProperty)
                lines = getNumLines(value);
            //theTable.setRowHeight(i, (lineWidth*lines)+2);
            //int heightWanted = (int)area.getPreferredSize().getHeight();
              //      if(heightWanted != theTable.getRowHeight(row));
                //    theTable.setRowHeight(row, heightWanted);
        }
        
        
        doChoiceBox();
        
        commentLabel.setFont(new Font("serif", Font.BOLD, 12));
        //theTable.setDefaultRenderer(String.class, new MultiLineCellRenderer());
        //theTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    
    public void setNameColumnWidth(int width)
    {
        //theTable.getColumnModel().getColumn(0).setMinWidth(width);
        //theTable.getColumnModel().getColumn(0).setMaxWidth(width);
        //theTable.getColumnModel().getColumn(0).setPreferredWidth(width);
        //theTable.getColumnModel().getColumn(0).setMaxWidth(width);
        //theTable.repaint();
    }
    
    private void doChoiceBox()
    {
        if(theModel.getNumGroups() == 0)
        {
            topPanel.removeAll();
            JLabel lab = new JLabel("");
            topPanel.setLayout(new BorderLayout());
            topPanel.add(lab, BorderLayout.CENTER);
        }
        else if(theModel.getNumGroups() == 1)
        {
            topPanel.removeAll();
            JLabel lab = new JLabel(theCombo.getModel().getElementAt(0).toString());
            topPanel.setLayout(new BorderLayout());
            topPanel.add(lab, BorderLayout.CENTER);
        }
        else
        {
            topPanel.removeAll();
            topPanel.setLayout(new BorderLayout());
            topPanel.add(theCombo, BorderLayout.CENTER);
        }
        this.revalidate();
    }
    
    public void stopEditing()
    {
        if(ce != null)ce.stopEditing();
    }
    
    public void setCurrEditor(PropertyTable.PropertyCellEditor ce)
    {
		TableColumn column = theTable.getColumnModel().getColumn(1);
		column.setCellEditor(ce);
		//if(this.ce != ce) System.out.println("THE CURREDITOR HAS CHANGED");
		
        this.ce = ce;
    }
    
    private PropertyTable.PropertyCellEditor ce;
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        javax.swing.JPanel jPanel1;
        javax.swing.JPanel jPanel3;
        javax.swing.JScrollPane jScrollPane1;
        javax.swing.JSplitPane jSplitPane1;

        jPanel1 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jScrollPane1.getViewport().setBackground(Color.white);
        theTable = new JTable()
        {

			


            //This method is a fix from http://www.codeguru.com/java/articles/180.shtml by Zafir Anjum, cheers!
            //this is required because there is a bug in JTable where the
            //just saying tableScroll.setColumnHeader(null); does not work as it should
            //Unfortunately, it overrides a deprecated API, so let's hope they
            //sort it out by Java 5.0, nice one Sun...
			//Addition (6th December 2004) This is no longer needed,
            /*protected void configureEnclosingScrollPane()
            {
                Container p = getParent();
                if (p instanceof JViewport)
                {
                    Container gp = p.getParent();
                    if (gp instanceof JScrollPane)
                    {
                        JScrollPane scrollPane = (JScrollPane)gp;
                        // Make certain we are the viewPort's view and not, for
                        // example, the rowHeaderView of the scrollPane -
                        // an implementor of fixed columns might do this.
                        JViewport viewport = scrollPane.getViewport();
                        if (viewport == null || viewport.getView() != this)
                        {
                            return;
                        }
                        //                scrollPane.setColumnHeaderView(getTableHeader());
                        scrollPane.getViewport().setBackingStoreEnabled(true);
                        scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
                    }
                }
            }*/

        };
        theTable.setModel(theModel);
        theTable.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        theTable.setRowSelectionAllowed(false);
        theTable.setColumnSelectionAllowed(false);
        theTable.setCellSelectionEnabled(true);
        jPanel3 = new javax.swing.JPanel();
        commentText = new javax.swing.JTextArea();
        commentLabel = new javax.swing.JLabel();
        topPanel = new javax.swing.JPanel();
        theCombo = new javax.swing.JComboBox();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setBackground(new java.awt.Color(255, 255, 255));
        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerSize(3);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setOneTouchExpandable(true);
        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setBorder(new javax.swing.border.LineBorder(java.awt.SystemColor.textInactiveText));
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        theTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String []
            {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        theTable.setDoubleBuffered(true);
        theTable.setGridColor(new java.awt.Color(198, 197, 197));
        jScrollPane1.setViewportView(theTable);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(new javax.swing.border.LineBorder(java.awt.SystemColor.inactiveCaption));
        jPanel3.setMinimumSize(new java.awt.Dimension(10, 75));
        jPanel3.setPreferredSize(new java.awt.Dimension(100, 75));
        commentText.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        commentText.setColumns(1);
        commentText.setEditable(false);
        commentText.setLineWrap(true);
        commentText.setWrapStyleWord(true);
        commentText.setBorder(null);
        commentText.setDoubleBuffered(true);
        commentText.setFocusable(false);
        commentText.setMinimumSize(new java.awt.Dimension(100, 75));
        commentText.setPreferredSize(new java.awt.Dimension(100, 75));
        jPanel3.add(commentText, java.awt.BorderLayout.CENTER);

        jPanel3.add(commentLabel, java.awt.BorderLayout.NORTH);

        jSplitPane1.setRightComponent(jPanel3);

        jPanel1.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        add(jPanel1, java.awt.BorderLayout.CENTER);

        topPanel.setLayout(new java.awt.BorderLayout());

        topPanel.add(theCombo, java.awt.BorderLayout.NORTH);

        add(topPanel, java.awt.BorderLayout.NORTH);

    }//GEN-END:initComponents
    
    public void valueChanged(ListSelectionEvent e)
    {
        ////System.out.println("VALUE CHANGED");
        SingleProperty selected = theModel.getSelectedProperty(theTable.getSelectedRow());
        
        if(selected != null)
        {
            commentLabel.setText(selected.getName());
            commentText.setText(selected.getComment());
        }
        else
        {
            commentLabel.setText("");
            commentText.setText("");
        }
        
        for(int i = 0; i < theModel.getRowCount(); i++)
        {
            ////System.out.println("Row "+i);
            String value = theModel.getValueAt(i, 1).toString();
            int lines = 1;
            
            if(theModel.getValueAt(i, 1) instanceof FontColorProperty)
            {
                int height = ((FontColorProperty)theModel.getValueAt(i,1)).getFontColorPair().f.getSize();
                height = Math.max(height, (lineWidth-2));
                theTable.setRowHeight(i, (height*lines)+4);
            }
            else if(theModel.getValueAt(i, 1) instanceof SingleProperty)
            {
                //lines = getNumLines(value);
                //int heightWanted = 
                //theTable.setRowHeight(i, (lineWidth*lines)+2);
            }
        }
    }
    
    public void tableChanged(TableModelEvent e)
    {
        ////System.out.println("TABLE CHANGED");
        CellEditor ce = theTable.getCellEditor();
        if(ce != null) ce.cancelCellEditing();
        theCombo.setModel(theModel.getComboModel());
        for(int i = 0; i < theModel.getRowCount(); i++)
        {
            ////System.out.println("Row "+i);
            String value = theModel.getValueAt(i, 1).toString();
            int lines = 1;
            
            if(theModel.getValueAt(i, 1) instanceof FontColorProperty)
            {
                int height = ((FontColorProperty)theModel.getValueAt(i,1)).getFontColorPair().f.getSize();
                
                height = Math.max(height, (lineWidth-2));
                theTable.setRowHeight(i, (height*lines)+4);
            }
            else if(theModel.getValueAt(i, 1) instanceof SingleProperty)
            {
                //lines = getNumLines(value);
                //theTable.setRowHeight(i, (lineWidth*lines)+2);
                //int heightWanted = (int)area.getPreferredSize().getHeight();
                  ///  if(heightWanted != theTable.getRowHeight(row));
                   // theTable.setRowHeight(row, heightWanted);
            }
            else if(theModel.getValueAt(i, 1) instanceof MultipleProperty)
            {
                lines = getNumLines(value);
                theTable.setRowHeight(i, (lineWidth*lines)+2);
            }
            
            
            
        }
        doChoiceBox();
        //theTable.s
    }
    
    public static int getNumLines(String str)
    {
        int count = 1;
        for(int i = 0; i < str.length(); i++)
        {
            char curr = str.charAt(i);
            if(curr=='\n') count++;
        }
        ////System.out.println("count = "+count);
        return count;
    }
    
    public void itemStateChanged(ItemEvent e)
    {
        theModel.setCurrentGroup(theCombo.getSelectedIndex());
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JLabel commentLabel;
    javax.swing.JTextArea commentText;
    javax.swing.JComboBox theCombo;
    javax.swing.JTable theTable;
    javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
    
    
    public class PropertyCellEditor extends AbstractCellEditor implements TableCellEditor, CaretListener, KeyListener, ActionListener
    {
        JTextArea area; //For multiline SingleProperties
        JTextField field; //For single line SingleProperties
        boolean isMultiLine;
        int currentRow;
        BooleanProperty boolProp = null;
        FontColorProperty fcProp = null;
        ChoiceProperty chProp = null;
        ColourProperty colProp = null;
        SeriesDataProperty seriesProp = null;
        
        public PropertyCellEditor()
        {
			//System.out.println("Instantiating new CELLEDITOR");
            area = new JTextArea();
            field = new JTextField();
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setOpaque(true);
            field.setOpaque(true);
            field.addCaretListener(this);
            area.addCaretListener(this);
            area.addKeyListener(this);
            
            area.setBackground(Color.white);
            setCurrEditor(this);
        }
        
        public void stopEditing()
        {
			//System.out.println(field.getParent().toString());
			//if(field != null) System.out.println("This should stop the editing of the box " +field.getText());
            super.stopCellEditing();
        }
        
        public boolean shouldSelectCell(EventObject anEvent)
        {
            area.selectAll();
            field.selectAll();
            return true;
        }
        
        public Object getCellEditorValue()
        {
            if(boolProp != null)
            {
                //System.out.println("getting boolean cell editor value");
                boolProp.removeListenerFromEditor(this);
                return boolProp.getEditorValue();
            }
            if(fcProp != null)
            {
                //System.out.println("getting font colour cell editor value");
                fcProp.removeListenerFromEditor(this);
                return fcProp.getEditorValue();
            }
            if(chProp != null)
            {
                //System.out.println("getting choice cell editor value");
                chProp.removeListenerFromEditor(this);
                return chProp.getEditorValue();
            }
            if(colProp != null)
            {
                colProp.removeListenerFromEditor(this);
                return colProp.getEditorValue();
            }
            if(seriesProp != null)
            {
                seriesProp.removeListenerFromEditor(this);
                return null;
            }
            if(isMultiLine)return area.getText();
            else return field.getText();
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
			//System.out.println("Calling getTableCellEditorComponent()");
            currentRow = row;
            boolProp = null;
            fcProp = null;
            chProp = null;
            colProp = null;
            if(value instanceof BooleanProperty)
            {
                boolProp = (BooleanProperty)value;
                boolProp.addListenerToEditor(this);
                return boolProp.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            else if(value instanceof FontColorProperty)
            {
                fcProp = (FontColorProperty)value;
                fcProp.addListenerToEditor(this);
                return fcProp.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            else if(value instanceof ChoiceProperty)
            {
                chProp = (ChoiceProperty)value;
                chProp.addListenerToEditor(this);
                return chProp.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            else if(value instanceof ColourProperty)
            {
                colProp = (ColourProperty)value;
                colProp.addListenerToEditor(this);
                return colProp.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            else if(value instanceof SeriesDataProperty)
            {
                seriesProp = (SeriesDataProperty)value;
                seriesProp.addListenerToEditor(this);
                return seriesProp.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            else if(value instanceof SingleProperty)
            {
                
                SingleProperty sp = (SingleProperty)value;
                if(!sp.isMultiline())
                {
					if(!sp.isEnabled())
					{/*
                    area.setBackground(new Color(250,250,250));
                    area.setForeground(Color.darkGray);
                    field.setBackground(new Color(250,250,250));
                    field.setForeground(Color.darkGray);
                 */
						field.setEnabled(false);
						area.setEnabled(false);
						field.setEditable(false);
						area.setEditable(false);
						field.setCaretColor(Color.white);
						area.setCaretColor(Color.white);
					}
					else
					{
						field.setEnabled(true);
						area.setEnabled(true);
						field.setEditable(true);
						area.setEditable(true);
						field.setCaretColor(Color.black);
						field.setCaretColor(Color.black);
					}
                    field.setFont(table.getFont());
                    field.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                    field.setForeground( UIManager.getColor("Table.focusCellForeground") );
                    field.setBackground( UIManager.getColor("Table.focusCellBackground") );
                    field.setMargin(new Insets(0, 2, 4, 2));
                    field.setText((value == null) ? "" : value.toString());
                    isMultiLine = false;
                    return field;
                }
                else
                {
					if(!sp.isEnabled())
					{/*
                    area.setBackground(new Color(250,250,250));
                    area.setForeground(Color.darkGray);
                    field.setBackground(new Color(250,250,250));
                    field.setForeground(Color.darkGray);
                 */
						field.setEnabled(false);
						area.setEnabled(false);
						field.setEditable(false);
						area.setEditable(false);
						field.setCaretColor(Color.white);
						area.setCaretColor(Color.white);
					}
					else
					{
						field.setEnabled(true);
						area.setEnabled(true);
						field.setEditable(true);
						area.setEditable(true);
						field.setCaretColor(Color.black);
						area.setCaretColor(Color.black);
					}
                    area.setFont(table.getFont());
                    area.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                    area.setForeground( UIManager.getColor("Table.focusCellForeground") );
                    area.setBackground( UIManager.getColor("Table.focusCellBackground") );
                    area.setMargin(new Insets(0, 2, 4, 2));
                    area.setText((value == null) ? "" : value.toString());
                    theTable.setEditingRow(row);
                    isMultiLine = true;
                    return area;
                }
            }
            else if(value instanceof MultipleProperty)
            {
                MultipleProperty sp = (MultipleProperty)value;
                if(sp.getProperty(0) instanceof BooleanProperty)
                {
                    boolProp = (BooleanProperty)sp.getProperty(0);
                    boolProp.addListenerToEditor(this);
                    return boolProp.getTableCellEditorComponentMulti(table, value, isSelected, row, column, sp.allTheSame());
                }
                else if(sp.getProperty(0) instanceof FontColorProperty)
                {
                    fcProp = (FontColorProperty)sp.getProperty(0);
                    fcProp.addListenerToEditor(this);
                    return fcProp.getTableCellEditorComponentMulti(table, value, isSelected, row, column, sp.allTheSame());
                }
                else if(sp.getProperty(0) instanceof ChoiceProperty)
                {
                    chProp = (ChoiceProperty)sp.getProperty(0);
                    chProp.addListenerToEditor(this);
                    return chProp.getTableCellEditorComponentMulti(table, value, isSelected, row, column, sp.allTheSame());
                }
                else if(sp.getProperty(0) instanceof ColourProperty)
                {
                    colProp = (ColourProperty)sp.getProperty(0);
                    colProp.addListenerToEditor(this);
                    return colProp.getTableCellEditorComponentMulti(table, value, isSelected, row, column, sp.allTheSame());
                }
                else if(sp.getProperty(0) instanceof SeriesDataProperty)
                {
                    seriesProp = (SeriesDataProperty)sp.getProperty(0);
                    seriesProp.addListenerToEditor(this);
                    return seriesProp.getTableCellEditorComponentMulti(table, value, isSelected, row, column, false, sp);
                }
                
                if(!sp.isMultiline())
                {
                    field.setFont(table.getFont());
                    field.setForeground( Color.lightGray );
                    field.setBackground( UIManager.getColor("Table.focusCellBackground") );
                    field.setForeground( Color.lightGray );
                    if(!sp.allTheSame())field.setBackground(new Color(240,240,240));
                    field.setMargin(new Insets(0, 2, 4, 2));
                    field.setText((value == null) ? "" : value.toString());
                    isMultiLine = false;
                    return field;
                }
                else
                {
                    area.setFont(table.getFont());
                    area.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                    area.setForeground( Color.lightGray );
                    area.setBackground( UIManager.getColor("Table.focusCellBackground") );
                    area.setForeground( Color.lightGray );
                    if(!sp.allTheSame())area.setBackground(new Color(240,240,240));
                    area.setMargin(new Insets(0, 2, 4, 2));
                    area.setText((value == null) ? "" : value.toString());
                    isMultiLine = true;
                    return area;
                }
            }
            else return new JLabel("ERROR");
        }
        
        
        public void caretUpdate(CaretEvent e)
        {
            
            //System.out.println("caret update");
            if(e.getSource() == field)
            {
                field.getCaret().setVisible(true);
            }
            if(e.getSource() == area)
            {
                //System.out.println("area");
                area.getCaret().setVisible(true);
                //theTable.setRowHeight(currentRow, (area.getLineCount()*lineWidth)+2);
                int heightWanted = (int)area.getPreferredSize().getHeight();
                    if(heightWanted != theTable.getRowHeight(currentRow))
                    theTable.setRowHeight(currentRow, heightWanted);
                
            }
        }
        
        public void keyPressed(KeyEvent e)
        {
            if(e.getSource() == area)
            {
                try
                {
                    int startOfLastLine = area.getLineStartOffset(area.getLineCount()-1);
                    int endOfFirstLine = area.getLineEndOffset(0);
                    int currentPosition = area.getCaretPosition();
                    if(e.getKeyCode() == KeyEvent.VK_UP)
                    {
                        if(currentPosition < endOfFirstLine)
                        {
                            //System.out.println("should move up");
                            this.stopCellEditing();
                            
                            int newPosition = (currentRow-1);
                            if(newPosition < 0) newPosition = theTable.getRowCount()-1;
                            theTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                        }
                        else if(area.getText().length() == 0)
                        {
                            this.stopCellEditing();
                            
                            int newPosition = (currentRow-1);
                            if(newPosition < 0) newPosition = theTable.getRowCount()-1;
                            theTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                        }
                    }
                    else if(e.getKeyCode() == KeyEvent.VK_DOWN)
                    {
                        if(currentPosition > startOfLastLine)
                        {
                            //System.out.println("should move down");
                            this.stopCellEditing();
                            int newPosition = (currentRow+1)%theTable.getRowCount();
                            theTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                        }
                        else if(area.getText().length() == 0)
                        {
                            this.stopCellEditing();
                            int newPosition = (currentRow+1)%theTable.getRowCount();
                            theTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                        }
                    }
                }
                catch(BadLocationException ex)
                {
                }
            }
        }
        
        public void keyReleased(KeyEvent e)
        {
        }
        
        public void keyTyped(KeyEvent e)
        {
        }
        
        
        public void actionPerformed(ActionEvent e)
        {
            stopCellEditing();
        }
        
    }
    
    //Uses different renderers for different types of property
    class PropertyCellRenderer implements TableCellRenderer
    {
        JTextArea area;
        JTextField field;
        
        public PropertyCellRenderer()
        {
            area = new JTextArea();
            field = new JTextField();
            area.setLineWrap(false);
            area.setOpaque(true);
            field.setOpaque(true);
            //area.setWrapStyleWord(f);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
        {
            ////System.out.println("rendering "+value.toString());
            if (isSelected)
            {
                area.setForeground(table.getSelectionForeground());
                area.setBackground(table.getSelectionBackground());
                field.setForeground(table.getSelectionForeground());
                field.setBackground(table.getSelectionBackground());
            }
            else
            {
                area.setForeground(table.getForeground());
                area.setBackground(Color.white);
                field.setForeground(table.getForeground());
                field.setBackground(Color.white);
            }
            
            
            
            
            if(value instanceof SingleProperty)
            {
                SingleProperty sp = (SingleProperty)value;
                Component c = sp.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(c != null) return c;
                
                if(!sp.isEnabled())
                {/*
                    area.setBackground(new Color(250,250,250));
                    area.setForeground(Color.darkGray);
                    field.setBackground(new Color(250,250,250));
                    field.setForeground(Color.darkGray);
                 */
                    field.setEnabled(false);
                    area.setEnabled(false);
					field.setEditable(false);
					area.setEditable(false);
					field.setCaretColor(Color.white);
					area.setCaretColor(Color.white);
                }
                else
                {
                    field.setEnabled(true);
                    area.setEnabled(true);
					field.setEditable(true);
					area.setEditable(true);
					field.setCaretColor(Color.black);
					area.setCaretColor(Color.black);
                }
                ////System.out.println("Rendering: "+sp.getName()+" "+sp.toString()+ " multiline = "+sp.isMultiline());
                if(!sp.isMultiline())
                {
                    field.setFont(table.getFont());
                    
                    if (hasFocus)
                    {
                        field.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                        if (table.isCellEditable(row, column))
                        {
                            field.setForeground( UIManager.getColor("Table.focusCellForeground") );
                            field.setBackground( UIManager.getColor("Table.focusCellBackground") );
                        }
                    }
                    else
                    {
                        field.setBorder(new EmptyBorder(1, 2, 2, 1));
                    }
                    field.setMargin(new Insets(0, 2, 4, 2));
                    field.setText((value == null) ? "" : value.toString());
                    return field;
                }
                else
                {
                    area.setFont(table.getFont());
                    
                    if (hasFocus)
                    {
                        area.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                        if (table.isCellEditable(row, column))
                        {
                            area.setForeground( UIManager.getColor("Table.focusCellForeground") );
                            area.setBackground( UIManager.getColor("Table.focusCellBackground") );
                        }
                    } else
                    {
                        area.setBorder(new EmptyBorder(0, 2, 2, 1));
                    }
                    //area.setBackground( Color.yellow);
                    area.setMargin(new Insets(0, 2, 4, 2));
                    area.setText((value == null) ? "" : value.toString());
                    
                    int heightWanted = (int)area.getPreferredSize().getHeight();
                    if(heightWanted != theTable.getRowHeight(row))
                    theTable.setRowHeight(row, heightWanted);
                    return area;
                }
            }
            else if(value instanceof MultipleProperty)
            {
                MultipleProperty sp = (MultipleProperty)value;
                ////System.out.println("Rendering:mult "+sp.getName()+" "+sp.toString()+ " multiline = "+sp.isMultiline());
                Component c = sp.getTableCellRendererComponentMulti(table, value, isSelected, hasFocus, row, column);
                if(c != null) return c;
                
                if(!sp.isMultiline())
                {
                    field.setFont(table.getFont());
                    
                    if (hasFocus)
                    {
                        field.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                        if (table.isCellEditable(row, column))
                        {
                            field.setForeground( Color.lightGray );
                            field.setBackground( UIManager.getColor("Table.focusCellBackground") );
                        }
                    }
                    else
                    {
                        field.setBorder(new EmptyBorder(1, 2, 2, 1));
                    }
                    
                    if(!sp.allTheSame())
                    {
                        field.setBackground(new Color(240,240,240));
                        field.setForeground( Color.lightGray );
                    }
                    else
                    {
                        field.setForeground(Color.black);
                    }
                    field.setMargin(new Insets(0, 2, 4, 2));
                    field.setText((value == null) ? "" : value.toString());
                    return field;
                }
                else
                {
                    area.setFont(table.getFont());
                    
                    if (hasFocus)
                    {
                        area.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                        if (table.isCellEditable(row, column))
                        {
                            area.setForeground( Color.lightGray );
                            area.setBackground( UIManager.getColor("Table.focusCellBackground") );
                        }
                    } else
                    {
                        area.setBorder(new EmptyBorder(0, 2, 2, 1));
                    }
                    
                    if(!sp.allTheSame())
                    {
                        area.setBackground(new Color(240,240,240));
                        area.setForeground( Color.lightGray );
                    }
                    else
                    {
                        area.setForeground(Color.black);
                    }
                    //area.setBackground(Color.yellow);
                    area.setMargin(new Insets(0, 2, 4, 2));
                    area.setText((value == null) ? "" : value.toString());
                    return area;
                }
            }
            else
            {
                field.setFont(table.getFont());
                
                if (hasFocus)
                {
                    field.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
                    if (table.isCellEditable(row, column))
                    {
                        field.setForeground( UIManager.getColor("Table.focusCellForeground") );
                        field.setBackground( UIManager.getColor("Table.focusCellBackground") );
                    }
                }
                else
                {
                    field.setBorder(new EmptyBorder(1, 2, 2, 1));
                }
                field.setMargin(new Insets(0, 2, 4, 2));
                field.setText((value == null) ? "" : value.toString());
                return field;
            }
        }
    }
    
    
}
