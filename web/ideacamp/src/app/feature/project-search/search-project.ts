import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { z } from 'zod';

/** Schema for validating project data */
const ProjectSchema = z.object({
  title: z.string(),
  description: z.string(),
  initials: z.string(),
});

/** Type inferred from the project schema */
type Project = z.infer<typeof ProjectSchema>;

/** Schema for validating a list of projects */
const ProjectListSchema = z.array(ProjectSchema);

/**
 * Schema for validating the search input
 *
 * The input may be empty
 * If it is not empty, it may only contain letters, numbers and spaces
 * The input must not start with a space and must not be spaces only
 */
const SearchInputSchema = z
  .string()
  .regex(/^[a-zA-ZäöüÄÖÜß0-9 ]*$/, {
    message: 'Only letters and numbers allowed!',
  })
  .refine(value => value.length === 0 || !value.startsWith(' '), {
    message: 'Search cannot start with a space.',
  });

/**
 * Displays the project search page
 *
 * The component validates the search input and filters the project list
 */
@Component({
  selector: 'app-search-project',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-project.html',
})
export class SearchProject {
  /** Stores the current value of the search input */
  searchTerm = '';

  /** Stores the validation error message shown below the search input */
  errorMessage = '';

  /**
   * Contains all available projects
   *
   * The project data is validated with Zod before it is used by the component
   */
  projects: Project[] = ProjectListSchema.parse([
    {
      title: 'Lorem Ipsum Project',
      description: 'Building the future of collaborative development',
      initials: 'LIP',
    },
    {
      title: 'E-commerce Platform',
      description: 'Modern shopping experience with cutting-edge technology',
      initials: 'EP',
    },
    {
      title: 'Project Dashboard',
      description: 'A dashboard for managing teams tasks and project progress',
      initials: 'PD',
    },
    {
      title: 'Mobile App Concept',
      description: 'A modern mobile app concept for users on the go',
      initials: 'MA',
    },
    {
      title: 'Portfolio Website',
      description: 'A clean personal website to showcase projects and skills',
      initials: 'PW',
    },
    {
      title: 'Task Manager',
      description: 'A simple tool for organizing daily tasks and deadlines',
      initials: 'TM',
    },
    {
      title: 'Booking System',
      description: 'A system for booking appointments and managing availability',
      initials: 'BS',
    },
    {
      title: 'Chat Application',
      description: 'A realtime chat app for teams and communities',
      initials: 'CA',
    },
    {
      title: 'Learning Platform',
      description: 'An online platform for courses lessons and student progress',
      initials: 'LP',
    },
    {
      title: 'Weather App',
      description: 'A small app that displays current weather and forecasts',
      initials: 'WA',
    },
  ]);

  /**
   * Contains the projects that are currently displayed
   *
   * By default, all projects are shown
   */
  filteredProjects: Project[] = this.projects;

  /**
   * Validates the search input and filters the project list
   *
   * If the input is empty, all projects are displayed
   * If the input is invalid, an error message is shown and no projects are displayed
   */
  onSearchChange(): void {
    const searchResult = SearchInputSchema.safeParse(this.searchTerm);

    if (!searchResult.success) {
      this.errorMessage =
        searchResult.error.issues[0]?.message ?? 'Invalid search input.';
      this.filteredProjects = [];
      return;
    }

    this.errorMessage = '';

    const value = searchResult.data.trim().toLowerCase();

    if (value === '') {
      this.filteredProjects = this.projects;
      return;
    }

    this.filteredProjects = this.projects.filter(project =>
      project.title.toLowerCase().includes(value) ||
      project.initials.toLowerCase().includes(value) ||
      this.descriptionContainsWholeWord(project.description, value)
    );
  }

  /**
   * Checks whether the project description contains the search value as a whole word
   *
   * This prevents partial matches in the description
   * For example, searching for "a" does not match the word "app" but searching for "app" does
   *
   * @param description - The project description that should be searched
   * @param searchValue - The normalized search value entered by the user
   * @returns True if the description contains the search value as a complete word or phrase
   */
  private descriptionContainsWholeWord(
    description: string,
    searchValue: string
  ): boolean {
    const descriptionWords = description
      .toLowerCase()
      .split(/[^a-zA-ZäöüÄÖÜß0-9]+/)
      .filter(Boolean);

    const searchWords = searchValue
      .toLowerCase()
      .split(' ')
      .filter(Boolean);

    return descriptionWords.some((_, index) =>
      searchWords.every(
        (searchWord, searchIndex) =>
          descriptionWords[index + searchIndex] === searchWord
      )
    );
  }
}
