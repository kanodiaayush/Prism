//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

//Java packages
package userinterface.model;

//PRISM packages

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import javax.swing.event.*;
import java.io.*;
import parser.*;


//Text editing panel with syntax highlighting of prism model files
/** This class extended JTextPane to give a text editor with highlighting features.  It also tells the GUIPrism of which
 * it is a member about modified events.
 */
public class GUITextModelEditor extends GUIModelEditor implements DocumentListener
{
	
	
	//Attributes
	
	private JEditorPane editor;
	private JComponent lineNumbers;
	private PlainDocument d;
	private GUIMultiModelHandler handler;
	
	
	
	
	//Constructor
	
	
	/** Creates a new GUIPrismTextEditor with the given text and the given parent
	 * GUIPrism.  The GUIPrism object is required for notification concerning
	 * modification events.
	 * @param str the text that should be placed in the editor originally.
	 * @param gui the parent GUIPrism.
	 */
	public GUITextModelEditor(String str, GUIMultiModelHandler handler)
	{
		editor = new JEditorPane();
		lineNumbers = new LineNumberPanel();
		this.handler = handler;
		PrismEditorKit kit = new PrismEditorKit();
		editor.setEditorKitForContentType("text/prism", kit);
		editor.setContentType("text/prism");
		editor.setBackground(Color.white);
		//editor.setFont(new Font("Monospaced", Font.PLAIN, 12));
		//PrismContext.addEditor(this);
		editor.setEditable(true);
		editor.setText(str);
		d = (PlainDocument)editor.getDocument();
		editor.getDocument().addDocumentListener(this);
		initComponents();
		
		
		
	}
	
	private void initComponents()
	{
		JScrollPane scroller = new JScrollPane();
		{
			scroller.setViewportView(editor);
			//scroller.setRowHeaderView(this.lineNumbers);
		}
		setLayout(new BorderLayout());
		add(scroller, BorderLayout.CENTER);
	}
	
	public void read(Reader s, Object obj) throws IOException
	{
		editor.read(s, obj);
		d = (PlainDocument)editor.getDocument();
		d.addDocumentListener(this);
	}
	
	public void setText(String str)
	{
		editor.setText(str);
	}
	
	public void write(Writer s) throws IOException
	{
		editor.write(s);
	}
	
	public String getTextString()
	{
		return editor.getText();
	}
	
	/**
	 * Resets the Model Editor
	 */
	public void newModel()
	{
		// note: we use the read() method instead of setText()
		// this avoids triggering the listener methods and hence unwanetd autoparsing
		try
		{
			read(new StringReader(""), "");
		} catch (IOException e)
		{}
	}
	
	//Listener Methods
	
	
	//Allows GUIPrismTextEditor to be a DocumentListener
	/** notifies the GUI that the document has
	 * been modified.
	 * @param e generated when there is a change in the document
	 */
	public void changedUpdate(DocumentEvent e)
	{
		//if (model != null)model.setTrue(GUIModel.MODIFIED);
		lineNumbers.repaint();
	}
	
	//Allows GUIPrismTextEditor to be a DocumentListener
	/** notifies the GUI that the document has
	 * been modified.
	 * @param e generated by an insert event to the document.
	 */
	public void insertUpdate(DocumentEvent e)
	{
		//System.out.println("insert update");
		//System.out.println("this.getWidth = "+getWidth());
		if (handler != null)handler.hasModified(true);
		
	}
	
	
	//Allows GUIPrismTextEditor to be a DocumentListener
	/** notifies the GUI that the document has
	 * been modified.
	 * @param e Generated when there is a remove event from the document.
	 */
	public void removeUpdate(DocumentEvent e)
	{
		//System.out.println("remove update");
		if (handler != null)handler.hasModified(true);
	}
	
	public String getParseText()
	{
		return editor.getText();
	}
	
	public void copy()
	{
		editor.copy();
	}
	
	public void cut()
	{
		editor.cut();
	}
	
	public void paste()
	{
		editor.paste();
	}
	
	public void delete()
	{
		try
		{
			editor.getDocument().remove(editor.getSelectionStart(), editor.getSelectionEnd()-editor.getSelectionStart());
		}
		catch(BadLocationException e)
		{
		}
	}
	
	public void selectAll()
	{
		editor.selectAll();
	}
	
	public boolean isEditable()
	{
		return editor.isEditable();
	}
	
	public void setEditable(boolean b)
	{
		editor.setEditable(b);
	}
	
	public void setEditorFont(Font f)
	{
		//System.out.println("font being set "+f);
		editor.setFont(f);
		this.lineNumbers.setFont(f);
	}
	
	public void setEditorBackground(Color c)
	{
		editor.setBackground(c);
	}
	
	class LineNumberPanel extends JComponent
	{
		
		public LineNumberPanel()
		{
			setBackground(Color.lightGray);
			setForeground(Color.black);
			setFont(editor.getFont());
		}
		
		public void paintComponent(Graphics g)
		{
			int lineHeight = fm.getHeight();
			int startOffset = this.getInsets().top + fm.getAscent();
			
			Rectangle area = g.getClipBounds();
			
			// Paint the background
			
			g.setColor( getBackground() );
			g.fillRect(area.x, area.y, area.width, area.height);
			
			//  Determine the number of lines to draw in the foreground.
			
			g.setColor( getForeground() );
			int startLineNumber = (area.y / lineHeight) + 1;
			int endLineNumber = Math.max(startLineNumber + (area.height / lineHeight), 15);
			
			int start = (area.y / lineHeight) * lineHeight + startOffset;
			
			for (int i = startLineNumber; i <= endLineNumber; i++)
			{
				String lineNumber = String.valueOf(i);
				int stringWidth = fm.stringWidth( lineNumber );
				int rowWidth = getSize().width;
				g.drawString(lineNumber, rowWidth - stringWidth - 5, start);
				start += lineHeight;
			}
			
			int rows = getSize().height / fm.getHeight();
			setWidthAccordingToLines( rows );
		}
		
		public void setWidthAccordingToLines(int numberOfLines)
		{
			int i = String.valueOf(numberOfLines).length();
			
			if (i != currentWidth && numberOfLines > 1)
			{
				currentWidth = numberOfLines;
				int width = fm.charWidth('0') * numberOfLines;
				Dimension d = getPreferredSize();
				d.setSize(2 * 5 + width, editor.getDocument().getRootElements().length * fm.getHeight());
				setPreferredSize( d );
				setSize( d );
			}
		}
		
		public void setFont(Font f)
		{
			super.setFont(f);
			fm = getFontMetrics( getFont() );
		}
		
		FontMetrics fm;
		int currentWidth;
	}
	
	class PrismEditorKit extends DefaultEditorKit
	{
		
		private PrismContext preferences;
		/** Creates a new instance of PrismEditorKit */
		public PrismEditorKit()
		{
			super();
		}
		
		public PrismContext getStylePreferences()
		{
			if (preferences == null)
			{
				preferences = new PrismContext();
			}
			return preferences;
		}
		
		public void setStylePreferences(PrismContext prefs)
		{
			preferences = prefs;
		}
		
		public String getContentType()
		{
			return "text/prism";
		}
		
		public Document createDefaultDocument()
		{
			return new PlainDocument();
		}
		
		public final ViewFactory getViewFactory()
		{
			return getStylePreferences();
		}
		
		
	}
	
	/*class NumberedViewFactory implements ViewFactory
	{
		public View create(Element elem)
		{
			String kind = elem.getName();
			if (kind != null)
				if (kind.equals(AbstractDocument.ContentElementName))
				{
					return new LabelView(elem);
				}
				else if (kind.equals(AbstractDocument.ParagraphElementName))
				{
					//      return new ParagraphView(elem);
					return new PrismView(elem);//NumberedParagraphView(elem);
				}
				else if (kind.equals(AbstractDocument.SectionElementName))
				{
					return new BoxView(elem, View.Y_AXIS);
				}
				else if (kind.equals(StyleConstants.ComponentElementName))
				{
					return new ComponentView(elem);
				}
				else if (kind.equals(StyleConstants.IconElementName))
				{
					return new IconView(elem);
				}
			// default to text display
			return new LabelView(elem);
		}
	}*/
	
	class PrismContext extends StyleContext implements ViewFactory
	{
		
		
		public static final String KEY_WORD_D = "Prism Keyword";
		public static final String NUMERIC_D = "Numeric";
		public static final String VARIABLE_D = "Variable";
		public static final String COMMENT_D = "Single Line Comment";
		
		/** Creates a new instance of PrismContext */
		public PrismContext()
		{
			super();
		}
		
		public View create(Element elem)
		{
			return new PrismView(elem);
		}
		
		
	}
	
	static final Style PLAIN_S = new Style(Color.black, Font.PLAIN);
	class PrismView extends PlainView
	{
		
		
		public PrismView(Element elem)
		{
			super(elem);
		}
		
		public void paint(Graphics g, Shape a)
		{
			super.paint(g, a);
		}
		
		/*protected void setInsets(short top, short left, short bottom,
		short right)
		{super.setInsets(top,(short)
		 (left+100),bottom,right);
		}
		 
		public int getPreviousLineCount()
		{
			int lineCount = 0;
			View parent = this.getParent();
			int count = parent.getViewCount();
			for (int i = 0; i < count; i++)
			{
				if (parent.getView(i) == this)
				{
					break;
				}
				else
				{
					lineCount += parent.getView(i).getViewCount();
				}
			}
			return lineCount;
		}
		 
		public void paintChild(Graphics g, Rectangle r, int n)
		{
			super.paintChild(g, r, n);
			int previousLineCount = getPreviousLineCount();
			int numberX = r.x - getLeftInset();
			int numberY = r.y + r.height - 2;
			g.drawString(Integer.toString(previousLineCount + n + 1),
			numberX, numberY);
		}*/
		
		protected int drawUnselectedText(Graphics g, int x, int y,int p0, int p1) throws BadLocationException
		{
			int stLine = p0;//findStartOfLine(p0, getDocument());
			int enLine = p1;//findEndOfLine(p1-1, getDocument());
			
			//x+= getLeftInset();
			//System.out.println("p0 = "+p0+", p1 = "+p1+", st = "+stLine+", enLine = "+enLine+".");
			try
			{
				g.setColor(Color.green);
				Document doc = getDocument();
				Segment segment = getLineBuffer();
				
				
				//System.out.println(doc.getText(p0, p1-p0));
				//String s = doc.getText(p0, p1-p0);
				String s = doc.getText(stLine, enLine-stLine);
				//System.out.println("------");
				//System.out.println("highlighting unselected string = \n"+s);
				//System.out.println("------");
				
				Style[] styles = highlight(s, (p0-stLine), (p1-p0));
				int currStart = 0;
				int currEnd = 0;
				Color last = null;
				String fname = handler.getPrismEditorFontFast().getName();
				int fsize = handler.getPrismEditorFontFast().getSize();
				
				for(int curr = 0; curr < styles.length; curr++)
				{
					
					Style c = styles[curr];
					
					g.setColor(c.c);
					g.setFont(new Font(fname, c.style, fsize));
					Segment segm = getLineBuffer();
					doc.getText(p0+curr, 1, segm);
					x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
					
				}
				g.setColor(Color.black);
				g.setFont(new Font(fname, Font.PLAIN, fsize));
			}
			catch(BadLocationException ex)
			{
				//System.out.println("ex = "+ex);
				//ex.printStackTrace();
			}
			return x;
		}
		
		protected int drawSelectedText(Graphics g, int x, int y,int p0, int p1) throws BadLocationException
		{
			int stLine = p0;//findStartOfLine(p0, getDocument());
			int enLine = p1;//findEndOfLine(p1-1, getDocument());
			
			//x+= getLeftInset();
			//System.out.println("p0 = "+p0+", p1 = "+p1+", st = "+stLine+", enLine = "+enLine+".");
			try
			{
				g.setColor(Color.green);
				Document doc = getDocument();
				Segment segment = getLineBuffer();
				
				
				//String s = doc.getText(p0, p1-p0);
				//System.out.println(doc.getText(p0, p1-p0));
				
				
				String s = doc.getText(stLine, enLine-stLine);
				//System.out.println("------");
				//System.out.println("highlighting selected string = \n"+s);
				//System.out.println("------");
				Style[] styles = highlight(s, (p0-stLine), (p1-p0));
				int currStart = 0;
				int currEnd = 0;
				Color last = null;
				String fname = handler.getPrismEditorFontFast().getName();
				int fsize = handler.getPrismEditorFontFast().getSize();
				
				for(int curr = 0; curr < styles.length; curr++)
				{
					
					Style c = styles[curr];
					
					g.setColor(c.c);
					g.setFont(new Font(fname, c.style, fsize));
					Segment segm = getLineBuffer();
					doc.getText(p0+curr, 1, segm);
					x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
					
				}
				g.setColor(Color.black);
				g.setFont(new Font(fname, Font.PLAIN, fsize));
			}
			catch(BadLocationException ex)
			{
				//System.out.println("ex = "+ex);
				//ex.printStackTrace();
			}
			return x;
		}
		
		private synchronized Style[] highlight(String s, int offset, int length)
		{
			int typeArray[];
			int i, n;
			if(!s.endsWith("\n"))
				s += "\n";
				//s = s.substring(0, s.length()-1);
			
			try
			{
				typeArray = PrismSyntaxHighlighter.lineForPrismGUI(s);
			}
			catch (ParseException e)
			{
				n = s.length();
				typeArray = new int[n];
				for (i = 0; i < n; i++)
					typeArray[i] = PrismSyntaxHighlighter.PUNCTUATION;
			}
			
			Style[] ret = new Style[length];
			for (i = 0; i < length; i++)
			{
				
				if(i+offset < typeArray.length)
				{
				switch (typeArray[i+offset])
				{
					case PrismSyntaxHighlighter.PUNCTUATION: ret[i] = PLAIN_S; break;
					case PrismSyntaxHighlighter.COMMENT: ret[i] = handler.getPrismEditorCommentFast(); break;
					case PrismSyntaxHighlighter.WHITESPACE: ret[i] = PLAIN_S; break;
					case PrismSyntaxHighlighter.KEYWORD: ret[i] = handler.getPrismEditorKeywordFast(); break;
					case PrismSyntaxHighlighter.NUMERIC: ret[i] = handler.getPrismEditorNumericFast(); break;
					case PrismSyntaxHighlighter.IDENTIFIER: ret[i] = handler.getPrismEditorVariableFast(); break;
					default: ret[i] = PLAIN_S; break;
				}
				}
				else ret[i] = PLAIN_S;
				
			}
			
			return ret;
		}
		
		private synchronized int findStartOfLine(int p0, Document d)
		{
			int index = p0;
			String s = "";
			try
			{
				s = d.getText(index, 1);
			}
			catch(BadLocationException e)
			{
				return 0;
			}
			index--;
			if(!(!s.equals("\n") && index >= -1)) index--;//botch of the century, an alternative good code
			while(!s.equals("\n") && index >= -1)
			{
				try
				{
					s = d.getText(index, 1);
				}
				catch(BadLocationException e)
				{
					return 0;
				}
				index--;
			}
			index+=2;
			return index;
		}
		
		private synchronized int findEndOfLine(int p1, Document d)
		{
			int index = p1;
			String s = "";
			try
			{
				s = d.getText(index, 1);
			}
			catch(BadLocationException e)
			{
				return d.getLength();
			}
			index++;
			while(!s.equals("\n") && index <= d.getLength())
			{
				try
				{
					s = d.getText(index, 1);
				}
				catch(BadLocationException e)
				{
					return d.getLength()-1;
				}
				index++;
			}
			index--;
			return index;
		}
		
		
		
	}
	
	
}
