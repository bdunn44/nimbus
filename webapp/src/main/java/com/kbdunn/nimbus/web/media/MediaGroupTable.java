package com.kbdunn.nimbus.web.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.kbdunn.nimbus.web.bean.MediaGroupBean;
import com.kbdunn.nimbus.web.bean.MediaGroupBeanQuery;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.media.MediaState;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Table;

public class MediaGroupTable extends Table implements Refreshable, ItemClickListener {

	private static final long serialVersionUID = -5709282242902994719L;
	
	private MediaGroupTableController controller;
	//private BeanItemContainer<MediaGroupBean> container;
	private LazyQueryContainer container;
	
	public MediaGroupTable(MediaGroupTableController controller) {
		addStyleName("n-file-table");
		this.controller = controller;
		
		addItemClickListener(this);
		setSelectable(true);
		setMultiSelect(false);
		setPageLength(0);
	}
	
	@Override
	public void refresh() {
		MediaState playerState = controller.getState(this);
		if (playerState == MediaState.VIDEOS || playerState == MediaState.SONGS) return;
		
		BeanQueryFactory<MediaGroupBeanQuery> queryFactory = new BeanQueryFactory<MediaGroupBeanQuery>(MediaGroupBeanQuery.class);
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(MediaGroupBeanQuery.CONTROLLER_KEY, controller);
		queryFactory.setQueryConfiguration(config);
		container = new LazyQueryContainer(queryFactory, MediaGroupBean.PROPERTY_ITEM_ID, 30, false);
		setContainerDataSource(container);
		
		container.addContainerProperty(MediaGroupBean.PROPERTY_NAME, String.class, "");
		setVisibleColumns(MediaGroupBean.PROPERTY_NAME);
		setColumnHeaders(playerState.display());
		
		//container.sort(new Object[] {MediaGroupBean.PROPERTY_NAME}, new boolean[] {true});
		setPageLength();
	}
	
	private void setPageLength() {
		if (size() > 15) {
			setPageLength(15);
		} else {
			setPageLength(0);
		}
	}
	
	public List<Object> getSelectedItems() {
		ArrayList<Object> result =  new ArrayList<Object>();
		if (getValue() == null) return result;
		
		if (getValue() instanceof MediaGroupBean) {
			result.add(((MediaGroupBean) getValue()).getMediaGroup());
		} else {
			Collection<?> currentItems = (Collection<?>) getValue(); 
			if (currentItems.size() > 0) {
				for (Object o: currentItems) {
					result.add(((MediaGroupBean) o).getMediaGroup());
				}
			}
		}
		
		return result;
	}
	
	@Override
	public void itemClick(ItemClickEvent event) {
		controller.handleClick(this, (MediaGroupBean) event.getItemId(), event.isDoubleClick());
	}
	
	public interface MediaGroupTableController {
		public MediaState getState(MediaGroupTable instance);
		public void handleClick(MediaGroupTable instance, MediaGroupBean clicked, boolean isDoubleClick);
	}
}
