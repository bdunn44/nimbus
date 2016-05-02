package com.kbdunn.nimbus.web.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.bean.SongBean;
import com.kbdunn.nimbus.web.bean.SongBeanQuery;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.media.MediaState;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

public class SongTable extends Table implements Refreshable, ItemClickListener {

	private static final long serialVersionUID = 7127548006481739584L;
	
	protected NimbusUser user;
	protected SongTableController controller;
	protected LazyQueryContainer container;
	
	public SongTable(SongTableController controller) {
		addStyleName("n-file-table");
		this.user = (NimbusUser) UI.getCurrent().getSession().getAttribute("user");
		this.controller = controller;
		addItemClickListener(this);
		
		BeanQueryFactory<SongBeanQuery> queryFactory = new BeanQueryFactory<SongBeanQuery>(SongBeanQuery.class);
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(SongBeanQuery.CONTROLLER_KEY, controller);
		queryFactory.setQueryConfiguration(config);
		container = new LazyQueryContainer(queryFactory, SongBean.PROPERTY_ITEM_ID, 30, false);
		setContainerDataSource(container);
		
		container.addContainerProperty(SongBean.PROPERTY_TRACK_NO, Integer.class, 1, true, false);
		container.addContainerProperty(SongBean.PROPERTY_TITLE, String.class, "", true, false);
		container.addContainerProperty(SongBean.PROPERTY_LENGTH, String.class, "", true, false);
		container.addContainerProperty(SongBean.PROPERTY_ARTIST, String.class, "", true, false);
		container.addContainerProperty(SongBean.PROPERTY_ALBUM, String.class, "", true, false);
		container.addContainerProperty(SongBean.PROPERTY_ALBUM_YEAR, String.class, "", true, false);
		
		setColumnExpandRatio(SongBean.PROPERTY_TRACK_NO, .5f);
		setColumnExpandRatio(SongBean.PROPERTY_TITLE, 3f);
		setColumnExpandRatio(SongBean.PROPERTY_LENGTH, .75f);
		setColumnExpandRatio(SongBean.PROPERTY_ARTIST, 2f);
		setColumnExpandRatio(SongBean.PROPERTY_ALBUM, 3.5f);
		setColumnExpandRatio(SongBean.PROPERTY_ALBUM_YEAR, .5f);
		
		setSelectable(true);
		setMultiSelect(true);
		setPageLength(0);
	}
	
	@Override
	public void refresh() {
		
		MediaState state = controller.getState(this);
		
		if (state == MediaState.SONGS || state == MediaState.PLAYLISTS) {
			setVisibleColumns(new Object[] {
					SongBean.PROPERTY_TITLE, SongBean.PROPERTY_LENGTH, SongBean.PROPERTY_ARTIST, 
					SongBean.PROPERTY_ALBUM, SongBean.PROPERTY_ALBUM_YEAR
				});
			setColumnHeaders(new String [] { "Title", "Length", "Artist", "Album", "Year"});
			setColumnAlignments(Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT);
			
		} else if (state == MediaState.ALBUMS || state == MediaState.ARTISTS) {
			setVisibleColumns(new Object[] {
					SongBean.PROPERTY_TRACK_NO, SongBean.PROPERTY_TITLE, SongBean.PROPERTY_LENGTH, SongBean.PROPERTY_ARTIST, 
					SongBean.PROPERTY_ALBUM, SongBean.PROPERTY_ALBUM_YEAR
				});
			setColumnHeaders(new String [] { "#", "Title", "Length", "Artist", "Album", "Year"});
			setColumnAlignments(Align.RIGHT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT);
		}
		container.refresh();
		setPageLength();
	}
	
	public void unselectAll() {
		for (Object selected : (Collection<?>) getValue())
			unselect(selected);
	}
	
	public List<NimbusFile> getContents() {
		return toNFile(getItemIds());
	}
	
	public List<NimbusFile> getSelected() {
		return toNFile(getValue());
	}
	
	private void setPageLength() {
		if (size() > 15) {
			setPageLength(15);
		} else {
			setPageLength(0);
		}
	}
	
	private List<NimbusFile> toNFile(Object o) {
		Collection<?> items = (Collection<?>) o;
		ArrayList<NimbusFile> result =  new ArrayList<NimbusFile>();
		// Single select is 0 length collection
		if (items.size() > 0) {
			for (Object item: items) {
				if (item instanceof SongBean)
					result.add(((SongBean) item).getSong());
			}
		} else {
			if (o instanceof SongBean)
				result.add(((SongBean) o).getSong());
		}
		return result;
	}

	@Override
	public void itemClick(ItemClickEvent event) {
		Object id = event.getItemId();
		if (id instanceof SongBean)
			controller.handleClick(this, (SongBean) id, event.isDoubleClick());
	}
	
	public interface SongTableController {
		public MediaState getState(SongTable instance);
		public void handleClick(SongTable instance, SongBean clicked, boolean isDoubleClick);
	}
}