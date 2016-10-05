package org.corebounce.soundium;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public abstract class TabPanel implements SelectionListener {
	private TabFolder  folder;
	private int        tabIdx;
	private String     label;

	protected TabPanel(String label) {
		this.label = label;
	}

	public TabItem createTabPanel(TabFolder folder) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		this.folder = folder;
		tabIdx   = folder.getItemCount();

		Composite panel = new Composite(folder, SWT.NONE);

		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		panel.setLayout(layout);

		fillContent(panel);
		
		TabItem result = new TabItem(folder, SWT.NONE);
		result.setControl(panel);
		result.setText(label);
		return result;
	}

	protected abstract void fillContent(Composite panel) throws LineUnavailableException, IOException, UnsupportedAudioFileException;
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		folder.setSelection(tabIdx);
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getTabIndex() {
		return tabIdx;
	}
}
