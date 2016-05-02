package com.kbdunn.nimbus.web.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import com.kbdunn.nimbus.common.model.Video;

public class VideoBeanQuery extends AbstractBeanQuery<VideoBean> {

	private static final long serialVersionUID = 4067573247415399377L;
	public static final String CONTROLLER_KEY = "controller";
	
	public VideoBeanQuery(QueryDefinition definition, Map<String, Object> queryConfiguration, 
			Object[] sortPropertyIds, boolean[] sortStates) {
		super(definition, queryConfiguration, sortPropertyIds, sortStates);
	}
	
	@Override
	protected VideoBean constructBean() {
		return new VideoBean();
	}
	
	@Override
	protected List<VideoBean> loadBeans(int startIndex, int count) {
		List<VideoBean> beans = new ArrayList<VideoBean>();
		for (Video v : getController().getCurrentVideos(this, startIndex, count)) {
			beans.add(new VideoBean(v));
		}
		return beans;
	}
	
	@Override
	protected void saveBeans(List<VideoBean> added, List<VideoBean> modified, List<VideoBean> removed) {
		throw new UnsupportedOperationException("Not implemented!");
	}
	
	@Override
	public int size() {
		return getController().getCurrentVideoCount(this);
	}
	
	private VideoBeanQueryController getController() {
		VideoBeanQueryController c = (VideoBeanQueryController) getQueryConfiguration().get(CONTROLLER_KEY);
		if (c == null) throw new IllegalStateException("The query configuration does not contain the " + CONTROLLER_KEY + " key");
		return c;
	}
	
	public interface VideoBeanQueryController {
		public List<Video> getCurrentVideos(VideoBeanQuery instance, int startIndex, int count);
		public int getCurrentVideoCount(VideoBeanQuery instance);
	}
}
