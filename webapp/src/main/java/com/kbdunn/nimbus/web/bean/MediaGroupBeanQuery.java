package com.kbdunn.nimbus.web.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.kbdunn.nimbus.common.model.MediaGroup;

public class MediaGroupBeanQuery extends AbstractBeanQuery<MediaGroupBean> {

	private static final long serialVersionUID = 4067573247415399377L;
	public static final String CONTROLLER_KEY = "controller";
	
	public MediaGroupBeanQuery(QueryDefinition definition, Map<String, Object> queryConfiguration, 
			Object[] sortPropertyIds, boolean[] sortStates) {
		super(definition, queryConfiguration, sortPropertyIds, sortStates);
	}
	
	@Override
	protected MediaGroupBean constructBean() {
		return new MediaGroupBean();
	}
	
	@Override
	protected List<MediaGroupBean> loadBeans(int startIndex, int count) {
		List<MediaGroupBean> beans = new ArrayList<MediaGroupBean>();
		for (MediaGroup g : getController().getCurrentMediaGroups(this, startIndex, count)) {
			beans.add(new MediaGroupBean(g));
		}
		return beans;
	}
	
	@Override
	protected void saveBeans(List<MediaGroupBean> added, List<MediaGroupBean> modified, List<MediaGroupBean> removed) {
		throw new UnsupportedOperationException("Not implemented!");
	}
	
	@Override
	public int size() {
		return getController().getCurrentMediaGroupCount(this);
	}
	
	private MediaGroupBeanQueryController getController() {
		MediaGroupBeanQueryController c = (MediaGroupBeanQueryController) getQueryConfiguration().get(CONTROLLER_KEY);
		if (c == null) throw new IllegalStateException("The query configuration does not contain the " + CONTROLLER_KEY + " key");
		return c;
	}
	
	public interface MediaGroupBeanQueryController {
		public List<MediaGroup> getCurrentMediaGroups(MediaGroupBeanQuery instance, int startIndex, int count);
		public int getCurrentMediaGroupCount(MediaGroupBeanQuery instance);
	}
}
