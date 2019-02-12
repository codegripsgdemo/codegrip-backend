package com.mb.codegrip.dao;

import java.util.List;

import com.mb.codegrip.dto.Quality;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CommitModel;

public interface QualityDAO {

	List<Quality> getQualityDetails(Integer branchId) throws CustomException;

	void updateCommitId(Quality quality, CommitModel commitModel);

	List<Quality> getQualityDetailsAsPerDays(Integer id, Integer i) throws CustomException;

	List<Quality> saveQualityRecord(List<Quality> qualities);

}
