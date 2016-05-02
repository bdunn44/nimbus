package com.kbdunn.nimbus.web.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.bean.VideoBean;
import com.kbdunn.nimbus.web.bean.VideoBeanQuery;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.media.MediaState;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Table;

public class VideoTable extends Table implements Refreshable, ItemClickListener {

	private static final long serialVersionUID = 5459821825790918054L;
	private VideoTableController controller;
	private LazyQueryContainer container;

	public VideoTable(VideoTableController controller) {
		this.controller = controller;
		
		BeanQueryFactory<VideoBeanQuery> queryFactory = new BeanQueryFactory<VideoBeanQuery>(VideoBeanQuery.class);
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(VideoBeanQuery.CONTROLLER_KEY, controller);
		queryFactory.setQueryConfiguration(config);
		container = new LazyQueryContainer(queryFactory, VideoBean.PROPERTY_ITEM_ID, 30, false);
		setContainerDataSource(container);
		container.addContainerProperty(VideoBean.PROPERTY_TITLE, String.class, "", true, false);
		setVisibleColumns(new Object[] {
				VideoBean.PROPERTY_TITLE //, VideoBean.PROPERTY_LENGTH, VideoBean.PROPERTY_YEAR
			});
			
		setColumnHeaders(new String [] { "Title"}); //, "Length", "Year"});
		setColumnAlignments(Align.LEFT); //, Align.LEFT, Align.LEFT);

		setSelectable(true);
		setMultiSelect(false);
		addItemClickListener(this);
	}
	
	@Override
	public void refresh() {
		if (controller.getState(this) != MediaState.VIDEOS) throw new IllegalStateException("Controller is not in video state");
		container.refresh();
		setPageLength();
	}
	
	private void setPageLength() {
		if (size() > 15) {
			setPageLength(15);
		} else {
			setPageLength(0);
		}
	}
	
	public void unselectAll() {
		for (Object selected : (Collection<?>) getValue())
			unselect(selected);
	}
	
	public List<NimbusFile> getSelected() {
		return toNFile(getValue());
	}
	
	private List<NimbusFile> toNFile(Object o) {
		Collection<?> items = (Collection<?>) o;
		ArrayList<NimbusFile> result =  new ArrayList<NimbusFile>();
		// Single select is 0 length collection
		if (items.size() > 0) {
			for (Object item: items) {
				if (item instanceof VideoBean)
					result.add(((VideoBean) item).getVideo());
			}
		} else {
			if (o instanceof VideoBean)
				result.add(((VideoBean) o).getVideo());
		}
		return result;
	}

	@Override
	public void itemClick(ItemClickEvent event) {
		Object id = event.getItemId();
		if (id instanceof VideoBean)
			controller.handleClick(this, (VideoBean) id, event.isDoubleClick());
	}
	
	public interface VideoTableController {
		public MediaState getState(VideoTable instance);
		public void handleClick(VideoTable instance, VideoBean clicked, boolean isDoubleClick);
	}
}