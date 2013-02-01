package mollusc.linguasubtitle;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import mollusc.linguasubtitle.subtitle.parser.Stem;

/**
 * @author mollusc <MolluscLab@gmail.com>
 */
public class CellRender extends DefaultTableCellRenderer{

    private ArrayList<String> hardWords;
    public CellRender(ArrayList<String> hardWords) {
	setOpaque(true);
	this.hardWords = hardWords;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	int indexRow = table.convertRowIndexToModel(row);
	boolean isKnown = (Boolean) table.getModel().getValueAt(indexRow, 2);
	boolean isStudy = (Boolean) table.getModel().getValueAt(indexRow, 1);
	int meeting = (Integer) table.getModel().getValueAt(indexRow, 6);
	
	Component c= super.getTableCellRendererComponent(table, value, isSelected, hasFocus, indexRow, column);
	boolean isHard = false;
	if(hardWords != null)
	{
	    Stem stem = new Stem((String)table.getValueAt(row, 3));
	    if(hardWords.contains(stem.getStem()))
		isHard = true;
	}
		
	paintCell(c, row, meeting, isKnown, isStudy, isHard);
	return c;
    }
    
    /**
     * Set background and foreground
     */
    static public void paintCell(Component component, int row, int meeting, boolean isKnown, boolean  isStudy, boolean isHard) {
	if (isKnown) {
	    if (meeting == 0) {
		component.setFont(component.getFont().deriveFont(Font.BOLD));
	    }
	    component.setBackground(Color.LIGHT_GRAY);
	    component.setForeground(Color.BLACK);
	} else {
	    if (meeting == 0) {
		component.setFont(component.getFont().deriveFont(Font.BOLD));
	    }
	    
	    if (row % 2 == 0) {
		component.setBackground(Color.white);
	    } else {
		component.setBackground(Color.decode("#eeeeee"));
	    }
	    
	    if(isStudy) component.setForeground(Color.decode("#007000"));
	    else if(isHard) component.setForeground(Color.RED);
	    else component.setForeground(Color.BLACK);
	}
    }
}