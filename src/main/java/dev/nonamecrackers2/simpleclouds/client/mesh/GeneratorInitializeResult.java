package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;

public class GeneratorInitializeResult
{
	private final GeneratorInitializeResult.State state;
	private final List<GeneratorInitializeResult.Error> errors;
	private @Nullable List<Path> savedReportsPaths;
	private @Nullable List<CrashReport> crashReports;
	
	private GeneratorInitializeResult(GeneratorInitializeResult.State state, List<GeneratorInitializeResult.Error> errors)
	{
		this.state = state;
		this.errors = errors;
	}
	
	public GeneratorInitializeResult.State getState()
	{
		return this.state;
	}
	
	public List<GeneratorInitializeResult.Error> getErrors()
	{
		return this.errors;
	}
	
	public List<CrashReport> createCrashReports()
	{
		List<CrashReport> reports = Lists.newArrayList();
		for (GeneratorInitializeResult.Error error : this.errors)
		{
			CrashReport report = CrashReport.forThrowable(error.error(), "Simple Clouds mesh generator initialization; " + error.title());
			CrashReportCategory category = report.addCategory("Initialization details");
			category.setDetail("Recommendation", error.text().getString());
			if (this.errors.size() > 1)
				category.setDetail("Notice", "Multiple crash reports have been generated during mesh generator initialization");
			reports.add(report);
		}
		this.crashReports = reports;
		return reports;
	}
	
	public void saveCrashReports(File gameDirectory)
	{
		this.savedReportsPaths = Lists.newArrayList();
		boolean flag = this.crashReports.size() > 1;
		for (int i = 0; i < this.crashReports.size(); i++)
		{
			CrashReport report = this.crashReports.get(i);
			File crashReportPath = new File(gameDirectory, "crash-reports");
			String fileName = "crash-" + Util.getFilenameFormattedDateTime() + "-simpleclouds-mesh-generator";
			if (flag)
				fileName += "-" + i + ".txt";
			else
				fileName += ".txt";
			File file = new File(crashReportPath, fileName);
			if (report.getSaveFile() == null)
			{
				report.saveToFile(file);
				this.savedReportsPaths.add(file.toPath());
			}
		}
	}
	
	public @Nullable List<Path> getSavedCrashReportPaths()
	{
		return this.savedReportsPaths;
	}
	
	public static GeneratorInitializeResult success()
	{
		return new GeneratorInitializeResult(GeneratorInitializeResult.State.SUCCESS, ImmutableList.of());
	}
	
	public static GeneratorInitializeResult.Builder builder()
	{
		return new GeneratorInitializeResult.Builder();
	}
	
	public static record Error(@Nullable Throwable error, String title, @Nullable Component text) {}
	
	public static enum State
	{
		SUCCESS,
		ERROR;
	}
	
	public static class Builder
	{
		private GeneratorInitializeResult.State state = GeneratorInitializeResult.State.SUCCESS;
		private final ImmutableList.Builder<GeneratorInitializeResult.Error> errors = ImmutableList.builder();
		
		private Builder() {}
		
		public Builder addError(@Nullable Throwable error, String title, Component text)
		{
			this.errors.add(new GeneratorInitializeResult.Error(error, title, Objects.requireNonNull(text)));
			this.state = GeneratorInitializeResult.State.ERROR;
			return this;
		}
		
		public Builder errorUnknown(@Nullable Throwable error, String title)
		{
			return this.addError(error, title, Component.translatable("gui.simpleclouds.error.unknown"));
		}
		
		public Builder errorRecommendations(@Nullable Throwable error, String title)
		{
			return this.addError(error, title, Component.translatable("gui.simpleclouds.error.recommendations"));
		}
		
		public Builder errorCouldNotLoadMeshScript(@Nullable Throwable error, String title)
		{
			return this.addError(error, title, Component.translatable("gui.simpleclouds.error.couldNotLoadMeshScript"));
		}
		
		public GeneratorInitializeResult build()
		{
			return new GeneratorInitializeResult(this.state, this.errors.build());
		}
	}
}
