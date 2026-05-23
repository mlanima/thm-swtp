import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/** Represents a project that can be displayed / searched on the search-project page */
type Project = {

  /** visible name of the project */
  title: string;

  /** short description explaining what the project is about */
  description: string;

  /** initials used as a visual placeholder inside the project avatar */
  initials: string;
};

/**
 * Displays the search-project page
 *
 * The component shows a searchable list of projects and validates the search input
 * Only letters, numbers and spaces are allowed
 */
@Component({
  selector: 'app-search-project',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-project.html'
})
export class SearchProject {
  /** Stores the current value of the search input */
  searchTerm = '';

  /**
   * Stores the validation error message shown below the search input
   *
   * If the input is valid, this value stays empty
   */
  errorMessage = '';

  /** Contains all available projects that can be shown on the page (later replaced through projects in backend) */
  projects: Project[] = [
    {
      title: 'Lorem Ipsum Projekt',
      description: 'Building the future of collaborative development',
      initials: 'LIP',
    },
    {
      title: 'Ecommerce Platform',
      description: 'Modern shopping experience with cutting-edge tech',
      initials: 'EP',
    },
    {
      title: 'Project Dashboard',
      description: 'A dashboard for managing team projects',
      initials: 'PD',
    },
    {
      title: 'Mobile App Concept',
      description: 'A modern app concept for mobile users',
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
      description: 'An online platform for courses, lessons and student progress',
      initials: 'LP',
    },
    {
      title: 'Weather App',
      description: 'A small app that displays current weather and forecasts',
      initials: 'WA',
    }
  ];

  /**
   * Contains the projects that are currently displayed
   *
   * By default, all projects are shown
   * This list is updated with matching projects, when the user enters a valid search term
   */
  filteredProjects: Project[] = this.projects;

  /**
   * Validates the search input and filters the project list
   *
   * The search allows letters, numbers and spaces only
   * If the input is empty, all projects are displayed
   * If the input is invalid, an error message is shown and no projects are displayed
   */
  onSearchChange(): void {
    const regex = /^[a-zA-ZäöüÄÖÜß0-9 ]*$/;

    if (!regex.test(this.searchTerm)) {
      this.errorMessage = 'Only letters and numbers allowed!';
      this.filteredProjects = [];
      return;
    }

    this.errorMessage = '';

    const value = this.searchTerm.trim().toLowerCase();

    if (value === '') {
      this.filteredProjects = this.projects;
      return;
    }

    this.filteredProjects = this.projects.filter(project =>
      project.title.toLowerCase().includes(value) ||
      project.description.toLowerCase().includes(value) ||
      project.initials.toLowerCase().includes(value)
    );
  }
}
